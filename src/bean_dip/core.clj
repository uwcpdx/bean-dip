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

(defn uc-first [s]
  (apply str
         (strs/upper-case (first s))
         (rest s)))

(defn split-on-hyphens [s]
  (-> (name s) (strs/split #"-")))

(defn hyphen->camel ^String [s]
  (let [words (split-on-hyphens s)]
    (apply str
           (first words)
           (map uc-first (rest words)))))

(defn hyphen->pascal ^String [s]
  (let [words (split-on-hyphens s)]
    (apply str (map uc-first words))))

(defn make-set-field-call [map-sym bean-sym get-method-name field-spec]
  (let [[field-key map-key-or-type-hint type-hint] (if (vector? field-spec)
                                                     field-spec
                                                     [field-spec field-spec nil])
        map-key     (if (keyword? map-key-or-type-hint)
                      map-key-or-type-hint
                      field-key)
        type-hint   (if (not (keyword? map-key-or-type-hint))
                      map-key-or-type-hint
                      type-hint)
        map-val-sym (if type-hint
                      (with-meta 'map-val {:tag type-hint})
                      'map-val)
        get-map-val (list `->bean-val
                          map-key
                          (list map-key map-sym))]
    (list `let [map-val-sym get-map-val]
          (list (symbol (get-method-name field-key))
                bean-sym
                map-val-sym))))

(defn make-set-seq [map-sym bean-sym field-specs]
  (into []
        (map (partial make-set-field-call
                      map-sym
                      bean-sym
                      #(str ".set" (hyphen->pascal %))))
        field-specs))

(defmacro def-map->bean [var-sym map-sym bean-class body]
  `(def ~(with-meta var-sym {:tag bean-class})
     (fn ~var-sym [~map-sym]
       ~body)))

(defmacro def-map->setter-bean [var-sym bean-class field-specs]
  (let [map-sym     'value-map
        bean-sym    'bean
        set-seq     (make-set-seq map-sym bean-sym field-specs)
        constructor (symbol (str bean-class "."))]
    `(def-map->bean
       ~var-sym
       ~map-sym
       ~bean-class
       (let [~bean-sym (~constructor)]
         ~@set-seq
         ~bean-sym))))

(defn make-build-seq [map-sym bean-sym field-specs]
  (into []
        (map (partial make-set-field-call
                      map-sym
                      bean-sym
                      #(str "." (hyphen->camel %))))
        field-specs))

(defmacro def-map->builder-bean [var-sym bean-class field-specs builder-form]
  (let [map-sym     'value-map
        builder-sym 'builder
        build-seq   (make-build-seq map-sym builder-sym field-specs)]
    `(def-map->bean
       ~var-sym
       ~map-sym
       ~bean-class
       (let [~builder-sym ~builder-form]
         ~@build-seq
         (.build ~builder-sym)))))

(defprotocol TranslatableToMap
  (bean->map [this]
    "Converts a Java bean to a map according to the key spec registered via extend-mappable
    (usually via deftranslation)"))

(defn name->getter [s]
  (str ".get" (hyphen->pascal s)))

(defn resolve-map-value [k v]
  (cond
    (nil? v)
    nil

    (extends? TranslatableToMap (class v))
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

(defn filter-specs [field-specs exclude-fields]
  (let [exclude? (set exclude-fields)]
    (into #{}
          (remove (fn [spec]
                    (exclude? (if (vector? spec)
                                (first spec)
                                spec))))
          field-specs)))

(defmacro def-translation
  "Defines functions for bidirectional translation between instances of the given bean class and
  maps. For translation from maps the function defined is named map->[MyBeanClass] (like defrecord
  creates), and for translation to maps it's [MyBeanClass]->map."
  [bean-class field-specs]
  (let [map->bean (symbol (str "map->" bean-class))
        bean->map (symbol (str bean-class "->map"))]
    `(do
       (extend-mappable ~bean-class ~field-specs)
       [(def-bean->map ~bean->map)
        (def-map->setter-bean ~map->bean ~bean-class ~field-specs)])))

(defmacro def-builder-translation [bean-class field-specs builder-form & exclude-fields]
  (let [map->bean (symbol (str "map->" bean-class))
        bean->map (symbol (str bean-class "->map"))]
    `(do
       (extend-mappable ~bean-class ~field-specs)
       [(def-bean->map ~bean->map)
        (def-map->builder-bean ~map->bean
                               ~bean-class
                               ~(filter-specs field-specs exclude-fields)
                               ~builder-form)])))

