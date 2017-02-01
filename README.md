# bean-dip

Bidirectional translation between Clojure maps and Java beans that's declarative and reflection-free.

## Basic Usage

For your dependencies:

`[uwcpdx/bean-dip "0.5.0"]`

Load core namespace and a trivial bean to demonstrate with in your REPL:

```
(require '[bean-dip.core :as bd])
(import [bean_dip TestBean])

(set! *warn-on-reflection* true)

; class TestBean {
;   Long fooField;
;   void setFooField(Long foo) { ... };
;   Long getBarField() { ... };
; }
```

Specify the keys to translate to and from a Java bean's fields and get two functions defined:

```
(bd/deftranslation TestBean #{:foo-field})
=> [#'user/TestBean->map #'user/map->TestBean]

(map->TestBean {:foo-field 42})
=> #object[bean_dip.TestBean 0x21bf6400 "TestBean{fooField=42}"]

(TestBean->map *1)
=> {:foo-field 42}
```

## Motivation / Pitch

To wrap a Java API involving numerous bean classes and instances, potentially deeply nested, translation to and from Clojure maps needs to be traceable, efficient and easy to maintain. Reflection can make translation automatic, resulting in less code to maintain, but it undermines traceability. Reflection can also become the bottleneck in your application when it's not cached and bean volumes are high enough. An explicit mapping allows you to trace a map key's bean provenance and also resolve bean accessor dispatch at compile time.

Existing translation solutions had feature gaps that lead us to create bean-dip: The `clojure.core/bean` built-in is one-way, uses uncached reflection and can't be configured. Cached reflection is available via [gavagai](https://github.com/ngrunwald/gavagai), but it's only one-way. There's [java.data](https://github.com/clojure/java.data), which is bidirectional, recursive and reflection-free, but it's not declarative making large translation layers hard to maintain.

Bean-dip is:

  * Bidirectional: Translate from beans to maps and back again
  * Declarative: Just specify the bean class and a set of keys for corresponding maps
  * Recursive: Translation descends into nested beans when they also have translations defined
  * Reflection-free: Macro generates type hinted code that's name checked at compile time via `*warn-on-reflection*`
  * Extensible: Translate values by key via implementations of multimethods

Namespaced keys are supported making it easy to enforce contracts with Java APIs using specs (more on this later).

## Extended Usage

Here are examples of all the features. Don't worry, there aren't many (just enough).

#### Key Aliasing

Map key names are translated to a bean field names by converting hyphens to camel case. If you want to use a different key in maps than you use for resolving field names, you can supply both in a vector (`[field-key map-key]`) instead of just the latter like so:

```
(bd/deftranslation TestBean #{[:foo-field :foo]})

(map->TestBean {:foo 42})
=> #object[bean_dip.TestBean 0x527171d7 "TestBean{fooField=42}"]

(TestBean->map *1)
=> {:foo 42}
```

#### Field Value Translation

Field values can be translated -- for example to implement (de)serialization -- by implementing two multi methods:

```
(defmethod bd/->bean-val :foo [_ v]
  (Long/parseLong v))

(defmethod bd/->map-val :foo [_ v]
  (str v))

(bd/deftranslation TestBean #{[:foo-field :foo]})

(map->TestBean {:foo "42"})
=> #object[bean_dip.TestBean 0x26b6426f "TestBean{fooField=42}"]

(TestBean->map *1)
=> {:foo "42"}
```

Java bean data models often have fields with the same name/type on different classes having identical semantics. The translation methods above being implemented by key allows you to consolidate handling of these fields, especially using namespaced keywords. If field names differ and semantics are identical, you can use key aliasing, as the translation is attached to the map key.

#### Recursive Translation

Currently bidirectional translation to and from Iterable and bean typed fields is supported. Nothing is built-in for Maps, but you could easily implement this via field value translations -- see above.

When translating from a Clojure map to a bean, implement `bean-dip.core/->bean-val` for keys that contain seqables to translate them to e.g. vectors of beans:

```
(import [bean_dip ParentBean])

; class ParentBean {
;   List<TestBean> children;
;   void setChildren(List<TestBean> children) { ... }
;   List<TestBean> getChildren() { ... }
; }

(bd/deftranslation TestBean #{:foo-field})
(bd/deftranslation ParentBean #{:children})

(defmethod bd/->bean-val :children [_ v]
  (mapv map->TestBean v))

(map->ParentBean {:children [{:foo-field 42}]})
=>
#object[bean_dip.ParentBean
        0x69e56629
        "ParentBean{children=[#object[bean_dip.TestBean 0x13b9d4de \"TestBean{fooField=42}\"]]}"]
```

When translating from a bean to a map, any Iterable or bean field value is descended into. Members and values whose type appears in a `bean-dip.core/deftranslation` evaluation will be translated as if by calling the corresponding *->map function on them (this is done via a protocol):

```
(ParentBean->map *1)
=> {:children [{:foo-field 42}]}
```

Any members of an Iterable or field values that are beans that don't have `bean-dip.core/deftranslation` evaluations are translated by the `bean-dip.core/->map-val` multimethod (which defaults to identity).

Note that `bean-dip.core/->bean-val` must be implemented for recursive translations from maps to beans, as no type inference is performed (or indeed is possible e.g. due to erasure in the Iterable case). The same doesn't go for `bean-dip.core/->map-val` and bean to map translation, as type checking allows automatic translation via protocol extension.

#### With Spec

Often bean data models have contracts expressed via Javadoc documentation, such as valid ranges for the values of specific fields. It's useful to codify these contracts between your Clojure code and the wrapped Java API by writing [specs](https://clojure.org/about/spec), which enable you to validate, generate and reason about the data being exchanged. Only the `name` of keys are used to find bean field names, so they can be namespaced keywords with specs attached:

```
(require '[clojure.spec :as s])
(require '[clojure.spec.test :as stest])

(s/def ::foo-field #(= % 42))
(s/def ::test-map (s/keys :req [::foo-field]))

(bd/deftranslation TestBean #{::foo-field})

; during test, validate values before they're passed to the wrapped API
(s/fdef map->TestBean :args (s/cat :test-map ::test-map))
(stest/instrument `map->TestBean)

(map->TestBean {::foo-field 41})
ExceptionInfo Call to #'user/map->TestBean did not conform to spec:
In: [0 :user/foo-field] val: 41 fails spec: :user/foo-field at: [:args :test-map :user/foo-field] predicate: (= % 42)
:clojure.spec/args  (#:user{:foo-field 41})
:clojure.spec/failure  :instrument
:clojure.spec.test/caller  {:file "form-init3400534552240922300.clj", :line 1, :var-scope user/eval6773}
  clojure.core/ex-info (core.clj:4725)

(map->TestBean {::foo-field 42})
=> #object[bean_dip.TestBean 0x48f45aaf "TestBean{fooField=42}"]
```

## Error Handling

If you specify a non-existent bean field with `*warn-on-reflection*` set to true, you'll receive a compiler warning:

```
(set! *warn-on-reflection* true)

(bd/deftranslation TestBean #{:bam})
Reflection warning, repl.clj:1:1 - reference to field getBam on bean_dip.TestBean can't be resolved.
Reflection warning, repl.clj:1:1 - call to method setBam on bean_dip.TestBean can't be resolved (no such method).
```

If you ignore this warning, you'll get a runtime error when attempting to use the translation:

```
(map->TestBean {:bam "ack"})
IllegalArgumentException No matching method found: setBam for class bean_dip.TestBean  clojure.lang.Reflector.invokeMatchingMethod (Reflector.java:53)
```

_If someone can show me how to enable `*warn-on-reflection*` within the closure of the macro expansion I'd be grateful!_

## How it Works

Bean-dip simply generates the type annotated invocations of getters/setters you'd write in a manual translation layer, with a dash of naming conventions and extension hooks sprinkled on top. Here's a logical expansion of a `deftranslation` form for illustration:

```
(magic-macroexpand '(deftranslation TestBean #{:foo-field}))
=>
(do
  (extend
    TestBean
    bean-dip.core/TranslatableToMap
    {:bean->map (fn [^TestBean bean]
                  (hash-map
                    :foo-field (bean-dip.core/resolve-map-value :foo-field
                                                                (.getFooField bean))))})
  [(defn TestBean->map [bean]
     (bean-dip.core/bean->map bean))
   (defn map->TestBean ^TestBean [value-map]
     (doto
       (new TestBean)
       (.setFooField (bean-dip.core/->bean-val :foo-field
                                               (:foo-field value-map)))))])
```

