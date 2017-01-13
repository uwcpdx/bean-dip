(ns bean-dip.core
  (:require [clojure.string :as strs]))

(defmulti ->for-bean
          "Used to convert values from map entries for setting on corresponding bean fields. Methods
          are implemented per map key and have the arg signature [key value]. Defaults to identity."
          (fn [k _] k))

(defmulti ->for-map
          "Used to convert values from bean fields for using in corresponding map entries. Methods
          are implemented per map key and have the arg signature [key value]. Defaults to identity."
          (fn [k _] k))

(defmethod ->for-bean :default [_ value] value)
(defmethod ->for-map :default [_ value] value)

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
          (list `->for-bean map-key value))))

(defn make-set-seq [value-map field-specs]
  (into []
        (map (partial make-set value-map))
        field-specs))

(defmacro map->bean [bean-class field-specs]
  (let [value-map   'value-map
        set-seq     (make-set-seq value-map field-specs)
        constructor (symbol (str bean-class "."))]
    `(fn [~value-map]
       (doto (~constructor)
         ~@set-seq))))

(defprotocol ConvertableToMap
  (->map [this]
    "Converts a Java bean to a map according to the key spec registered via set-translation!"))

(defn name->getter [s]
  (str ".get" (hyphen->camel s)))

(defn coerce-map-value [k v]
  (cond
    (nil? v)
    nil

    (extends? ConvertableToMap (type v))
    (->map v)

    (instance? Iterable v)
    (into [] (map ->map) v)

    :default
    (->for-map k v)))

(defn make-get [bean spec]
  (let [[field-key map-key] (if (vector? spec) spec [spec spec])
        get-value (list (symbol (name->getter field-key)) bean)]
    [map-key `(coerce-map-value ~map-key ~get-value)]))

(defn make-map [bean field-specs]
  (into '(hash-map)
        (mapcat (partial make-get bean))
        field-specs))

(defmacro bean->map [bean-class field-specs]
  (let [bean         (with-meta 'bean {:tag bean-class})
        make-map-seq (reverse (make-map bean field-specs))]
    `(fn [~bean]
       ~make-map-seq)))

(defmacro extend-mappable [bean-class field-specs]
  `(extend ~bean-class
     ConvertableToMap
     {:->map (bean->map ~bean-class ~field-specs)}))

(defmacro set-translation!
  "Extends ->map to the given bean class and returns a function for translating maps into instances
  of that bean class. Intended to be used to def a function by the name map->MyBeanClass."
  [bean-class field-specs]
  `(do
     (extend-mappable ~bean-class ~field-specs)
     (map->bean ~bean-class ~field-specs)))

