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

(defn uc-first [s]
  (apply str
         (strs/upper-case (first s))
         (rest s)))

(defn split-on-hyphens [s]
  (strs/split s #"-"))

(defn hyphen->camel ^String [s]
  (let [words (split-on-hyphens s)]
    (apply str
           (first words)
           (map uc-first (rest words)))))

(defn hyphen->pascal ^String [s]
  (let [words (split-on-hyphens s)]
    (apply str (map uc-first words))))

(defn qmarked? [s]
  (strs/ends-with? s "?"))

(defn deqmark [s]
  (subs s 0 (- (count s) 1)))

(defn maybe-deqmark [s]
  (if (qmarked? s)
    (deqmark s)
    s))

(defn make-set-field-call [map-sym bean-sym get-method-name field-spec]
  (let [[field-key map-key type-hint] field-spec
        map-val-sym (if type-hint
                      (with-meta 'map-val {:tag type-hint})
                      'map-val)
        get-map-val (list `->bean-val
                          map-key
                          (list map-key map-sym))]
    `(if (contains? ~map-sym ~map-key)
       (let ~[map-val-sym get-map-val]
         ~(list (symbol (get-method-name field-key))
                bean-sym
                map-val-sym)))))

(defn make-set-seq [map-sym bean-sym field-specs]
  (into []
        (map (partial make-set-field-call
                      map-sym
                      bean-sym
                      #(str ".set" (-> (name %)
                                       maybe-deqmark
                                       hyphen->pascal))))
        field-specs))

(defmacro def-map->setter-bean [var-sym bean-class-sym field-specs]
  (let [map-sym     'value-map
        bean-sym    'bean
        set-seq     (make-set-seq map-sym bean-sym field-specs)
        constructor (symbol (str bean-class-sym "."))]
    `(def ~(with-meta var-sym {:tag bean-class-sym})
       (fn ~var-sym [~map-sym]
         (let [~bean-sym (~constructor)]
           ~@set-seq
           ~bean-sym)))))

(defmulti builder-override
          "Override the default builder method invocation per field. Useful when builders have usage
           not covered by the single-arity-method-per-field-name convention."
          (fn [class-and-key & _] class-and-key))

(defn make-build-seq [map-sym builder-sym bean-class-sym bean-class field-specs]
  (into []
        (map (fn [[_ map-key :as spec]]
               (if (get-method builder-override [bean-class map-key])
                 `(builder-override [~bean-class-sym ~map-key]
                                    ~builder-sym
                                    (~map-key ~map-sym))
                 (make-set-field-call map-sym
                                      builder-sym
                                      #(str "." (-> (name %)
                                                    maybe-deqmark
                                                    hyphen->camel))
                                      spec))))
        field-specs))

(defmacro def-map->bean-builder [var-sym bean-class-sym builder-class-sym builder-form field-specs]
  (let [map-sym     'value-map
        builder-sym 'builder
        bean-class  (resolve bean-class-sym)
        build-seq   (make-build-seq map-sym builder-sym bean-class-sym bean-class field-specs)]
    `(def ~(with-meta var-sym {:tag builder-class-sym})
       (fn ~var-sym [~map-sym]
         (let ~[builder-sym builder-form]
           ~@build-seq
           ~builder-sym)))))

(defmacro def-map->bean-via-builder [map->bean map->builder bean-class-sym]
  (let [map-sym         'value-map
        map->build-call (list '.build
                              (list map->builder map-sym))]
    `(def ~(with-meta map->bean {:tag bean-class-sym})
       (fn ~map->bean [~map-sym]
         ~map->build-call))))

(defprotocol TranslatableToMap
  (bean->map [this]
    "Converts a Java bean to a map according to the key spec registered via extend-mappable
    (usually via deftranslation)"))

(defn name->getter-sym [field-key map-key]
  (-> (let [map-key-name (name map-key)]
        (if (qmarked? map-key-name)
          (str ".is" (-> map-key-name deqmark hyphen->pascal))
          (str ".get" (-> field-key name hyphen->pascal))))
      symbol))

(defn resolve-map-value [k v]
  (cond
    (get-method ->map-val k)
    (->map-val k v)

    (instance? Iterable v)
    (into [] (map bean->map) v)

    (extends? TranslatableToMap (class v))
    (bean->map v)

    :default
    v))

(defn make-maybe-assoc! [transient-sym bean-sym spec]
  (let [[field-key map-key] spec]
    `(when-let [value-sym# (~(name->getter-sym field-key map-key) ~bean-sym)]
       (assoc! ~transient-sym ~map-key (resolve-map-value ~map-key value-sym#)))))

(defn make-map [bean-sym field-specs]
  (let [transient-sym 'transient]
    (-> (into `([~transient-sym (transient {})] let)
              (map (partial make-maybe-assoc! transient-sym bean-sym))
              field-specs)
        (conj `(persistent! ~transient-sym)))))

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

(defn desugar-field-specs [field-specs]
  (into #{}
        (map (fn [spec]
               (let
                 [[field-key map-key-or-type-hint type-hint] (if (vector? spec)
                                                               spec
                                                               [spec spec nil])
                  map-key   (if (keyword? map-key-or-type-hint)
                              map-key-or-type-hint
                              field-key)
                  type-hint (if (not (keyword? map-key-or-type-hint))
                              map-key-or-type-hint
                              type-hint)]
                 [field-key map-key type-hint])))
        field-specs))

(defmacro def-translation
  "Defines functions for bidirectional translation between instances of the given bean class and
  maps. For translation from maps the function defined is named map->[MyBeanClass] (like defrecord
  creates), and for translation to maps it's [MyBeanClass]->map."
  [bean-class-sym field-specs]
  (let [map->bean   (symbol (str "map->" bean-class-sym))
        bean->map   (symbol (str bean-class-sym "->map"))
        field-specs (desugar-field-specs field-specs)]
    `(do
       (extend-mappable ~bean-class-sym ~field-specs)
       [(def-bean->map ~bean->map)
        (def-map->setter-bean ~map->bean ~bean-class-sym ~field-specs)])))

(defmacro def-builder-translation [bean-class-sym builder-class-sym field-specs & [{:keys [builder-form
                                                                                           exclude-fields]}]]
  (let [map->builder (symbol (str "map->" builder-class-sym))
        map->bean    (symbol (str "map->" bean-class-sym))
        bean->map    (symbol (str bean-class-sym "->map"))
        field-specs  (desugar-field-specs field-specs)
        builder-form (or builder-form
                         (list (symbol (str builder-class-sym) ".")))]
    `(do
       (extend-mappable ~bean-class-sym ~field-specs)
       [(def-bean->map ~bean->map)
        (def-map->bean-builder ~map->builder
                               ~bean-class-sym
                               ~builder-class-sym
                               ~builder-form
                               ~(filter-specs field-specs exclude-fields))
        (def-map->bean-via-builder ~map->bean
                                   ~map->builder
                                   ~bean-class-sym)])))

