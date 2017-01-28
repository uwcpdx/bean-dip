(ns bean-dip.core
  (:require [clojure.string :as strs]))

(defmulti ->bean-val
          "Used to translate values from map entries for setting on corresponding bean fields.
          Methods are implemented per map key and have the arg signature [key value]. Defaults to
          identity."
          (fn [k _] k))

(defmulti ->map-val
          "Used to translate values from bean fields for using in corresponding map entries. Methods
          are implemented per map key and have the arg signature [key value]. Defaults to identity."
          (fn [k _] k))

(defmethod ->bean-val :default [_ value] value)
(defmethod ->map-val :default [_ value] value)

(defn hyphen->camel ^String [s]
  (let [components (-> (name s) (strs/split #"-"))]
    (apply str (map #(apply str
                            (strs/upper-case (first %))
                            (rest %))
                    components))))

(defn name->setter [s]
  (str ".set" (hyphen->camel s)))

(defn make-set [value-map spec]
  (let [[field-key map-key] (if (vector? spec) spec [spec spec])
        value (list map-key value-map)]
    (list (symbol (name->setter field-key))
          (list `->bean-val map-key value))))

(defn make-set-seq [value-map field-specs]
  (into []
        (map (partial make-set value-map))
        field-specs))

(defmacro def-map->bean [var-sym bean-class field-specs]
  (let [value-map   'value-map
        set-seq     (make-set-seq value-map field-specs)
        constructor (symbol (str bean-class "."))]
    `(def ~(with-meta var-sym {:tag bean-class})
       (fn ~var-sym [~value-map]
         (doto (~constructor)
           ~@set-seq)))))

(defprotocol TranslatableToMap
  (bean->map [this]
    "Converts a Java bean to a map according to the key spec registered via extend-mappable
    (usually via deftranslation)"))

(defn name->getter [s]
  (str ".get" (hyphen->camel s)))

(defn resolve-map-value [k v]
  (cond
    (nil? v)
    nil

    (extends? TranslatableToMap (type v))
    (bean->map v)

    (instance? Iterable v)
    (into [] (map bean->map) v)

    :default
    (->map-val k v)))

(defn make-get [bean spec]
  (let [[field-key map-key] (if (vector? spec) spec [spec spec])
        get-value (list (symbol (name->getter field-key)) bean)]
    [map-key `(resolve-map-value ~map-key ~get-value)]))

(defn make-map [bean field-specs]
  (into '(hash-map)
        (mapcat (partial make-get bean))
        field-specs))

(defmacro make-bean->map [bean-class field-specs]
  (let [bean         (with-meta 'bean {:tag bean-class})
        make-map-seq (reverse (make-map bean field-specs))]
    `(fn [~bean]
       ~make-map-seq)))

(defmacro extend-mappable [bean-class field-specs]
  `(extend ~bean-class
     TranslatableToMap
     {:bean->map (make-bean->map ~bean-class ~field-specs)}))

(defmacro def-bean->map [var-sym]
  (let [bean 'bean]
    `(defn ~var-sym [~bean]
       (bean->map ~bean))))

(defmacro deftranslation
  "Defines functions for bidirectional translation between instances of the given bean class and
  maps. For translation from maps the function defined is named map->[MyBeanClass] (like defrecord
  creates), and for translation to maps it's [MyBeanClass]->map."
  [bean-class field-specs]
  (let [map->bean (symbol (str "map->" bean-class))
        bean->map (symbol (str bean-class "->map"))]
    `(do
       (extend-mappable ~bean-class ~field-specs)
       [(def-bean->map ~bean->map)
        (def-map->bean ~map->bean ~bean-class ~field-specs)])))

