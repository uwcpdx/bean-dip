# bean-dip

[![CircleCI](https://circleci.com/gh/uwcpdx/bean-dip/tree/master.svg?style=svg)](https://circleci.com/gh/uwcpdx/bean-dip/tree/master)

Bidirectional translation between Clojure maps and Java beans that's declarative and reflection-free.

## Basic Usage

For your dependencies:

`[uwcpdx/bean-dip "0.7.3"]`

Load core namespace and a trivial bean to demonstrate with in your REPL:

```
(require '[bean-dip.core :as bd])
(import [bean_dip BasicBean])

(set! *warn-on-reflection* true)

; class BasicBean {
;   Long fooField;
;   void setFooField(Long foo) { ... };
;   Long getFooField() { ... };
; }
```

Give the keys to translate to and from Java bean fields, get two defns:

```
(bd/def-translation BasicBean #{:foo-field})
=> [#'user/BasicBean->map #'user/map->BasicBean]

(map->BasicBean {:foo-field 42})
=> #object[bean_dip.BasicBean 0x19c1ea3f "BasicBean{fooField=42}"]

(BasicBean->map *1)
=> {:foo-field 42}
```

## Motivation / Pitch

To wrap a Java API involving numerous bean classes and instances, potentially deeply nested, translation to and from Clojure maps needs to be traceable, efficient and easy to maintain. Reflection can make translation automatic, resulting in less code to maintain, but it undermines traceability. Reflection can also become the bottleneck in your application when it's not cached and bean volumes are high enough. An explicit mapping allows you to trace a map key's bean provenance and also resolve bean accessor dispatch at compile time.

Existing translation solutions had feature gaps that lead us to create bean-dip: The `clojure.core/bean` built-in is one-way, uses uncached reflection and can't be configured. Cached reflection is available via [gavagai](https://github.com/ngrunwald/gavagai), but it's only one-way. There's [java.data](https://github.com/clojure/java.data), which is bidirectional, recursive and reflection-free, but it's not declarative making large translation layers hard to maintain.

Bean-dip is:

  * Bidirectional: Translate from beans to maps and back again
  * Declarative: Just specify the bean class and a set of keys for corresponding maps
  * Recursive: Translation descends into nested beans when they also have translations defined
  * Reflection-free: Macro generates type hinted code that can be name checked at compile time via `*warn-on-reflection*`
  * Extensible: Translate values by key via implementations of multimethods

Namespaced keys are supported making it easy to enforce contracts with Java APIs using specs (more on this later).

## Basic Features

Here are examples of some more basic features.

#### Key Aliasing

Map key names are translated to a bean field names by converting hyphens to camel case. If you want to use a different key in maps than you use for resolving field names, you can supply both in a vector, `[field-key map-key]`, instead of just one for both:

```
(bd/def-translation BasicBean #{[:foo-field :foo]})

(map->BasicBean {:foo 42})
=> #object[bean_dip.BasicBean 0x714e3971 "BasicBean{fooField=42}"]

(BasicBean->map *1)
=> {:foo 42}
```

#### Field Value Translation

Field values can be bidirectionally translated if their bean representation varies from their map one (e.g. serialization/deserialization). Just implement the `bean-dip.core/->bean-val` and `bean-dip.core/->map-val` multimethods for the key in question:

```
; maps use a string representation of the value and beans use a long

(defmethod bd/->bean-val :foo-field [_ v]
  (Long/parseLong v))

(defmethod bd/->map-val :foo-field [_ v]
  (str v))

(bd/def-translation BasicBean #{:foo-field})

(map->BasicBean {:foo-field "42"})
=> #object[bean_dip.BasicBean 0x10be0937 "BasicBean{fooField=42}"]

(BasicBean->map *1)
=> {:foo-field "42"}
```

Java bean data models often have fields with the same name/type on different classes having identical semantics. The translation methods above are implemented by key to consolidate handling of these fields, especially when using namespaced keywords. If field names differ and semantics are identical, you can use key aliasing, as *the translation is attached to the map key, not the bean field.*

#### Recursive Translation

Currently bidirectional translation to and from Iterable and bean typed fields is supported. Nothing is built-in for Maps, but you could easily implement this via field value translations -- see above.

When translating from a Clojure map to a bean, implement `bean-dip.core/->bean-val` for keys that contain seqables to translate them to e.g. vectors of beans:

```
(import [bean_dip ParentBean])

; class ParentBean {
;   List<BasicBean> children;
;   void setChildren(List<BasicBean> children) { ... }
;   List<BasicBean> getChildren() { ... }
; }

(bd/def-translation BasicBean #{:foo-field})
(bd/def-translation ParentBean #{:children})

(defmethod bd/->bean-val :children [_ v]
  (mapv map->BasicBean v))

(map->ParentBean {:children [{:foo-field 42}]})
=>
#object[bean_dip.ParentBean
        0x69e56629
        "ParentBean{children=[#object[bean_dip.BasicBean 0x13b9d4de \"BasicBean{fooField=42}\"]]}"]
```

When translating from a bean to a map, any Iterable or bean field value is descended into. Child beans whose type appears in a `bean-dip.core/def-translation` evaluation will be translated as if by calling the corresponding *->map function on them (via protocol):

```
(ParentBean->map *1)
=> {:children [{:foo-field 42}]}
```

Any children that don't have `bean-dip.core/def-translation` evaluations are translated by the `bean-dip.core/->map-val` multimethod (which defaults to identity).

Note that `bean-dip.core/->bean-val` must be implemented for recursive translations from maps to beans, as no type inference is performed (or indeed is possible e.g. due to erasure in the Iterable case). The same doesn't go for `bean-dip.core/->map-val` and bean to map translation, as type checking allows automatic translation via protocol extension.

#### Boolean Naming Convention

Java Beans have a naming convention for boolean valued fields as does Clojure for boolean keys and symbols. For a boolean field `someCondition` on a Java Bean, the getter will be `isSomeCondition` and the setter `setSomeCondition`. For this case you'd use a field key of `:some-condition?` with bean-dip.

### Immutable Beans and Builders

Some Java Beans one encounters are, admirably, immutable, and only instantiated by a "builder" class. Bean-dip supports constructing these beans to the extent their builder class conforms to common naming convention.

#### Basics

To define a builder translation, use `bean-dip.core/def-builder-translation`, which takes a builder class in addition to the usual `def-translation` arguments:

```
(import [bean_dip BuiltBean BuiltBean$Builder])

; class BuiltBean {
;   static Builder builder();
;   ...
;   static class Builder {
;     Builder fooField(Long foo);
;     BuiltBean build();
;     ...
;   }
; }

(main/def-builder-translation BuiltBean
                              BuiltBean$Builder
                              #{:foo-field}
                              {:builder-form (BuiltBean/builder)})
=> [#'user/BuiltBean->map #'user/map->BuiltBean$Builder #'user/map->BuiltBean]
```

When the builder class itself doesn't possess an empty constructor, use the `:builder-form` option as above to supply a form that returns an instance of it. (Note that this form is evaluated each time a fresh builder is needed, not on evaluation of `def-builder-translation`.)

The resulting `BuiltBean->map` and `map->BuiltBean` behave exactly as those defined by `def-translation`. i.e. All features work normally including extension via `->bean-val` and `->map-val`, recursion, and key aliasing.

Also defined by `def-builder-translation` is `map->BuiltBean$Builder`, which is useful if you need to do something with the builder instance other than call `.build` on it.

#### Unconventional Builder Methods

Builder classes don't always completely follow the "builder method per bean field name" convention. For example collection fields sometimes require a builder method invocation per item (instead of a single one passing a complete set). For these and other varieties of deviant, bean-dip provides the `bean-dip.core/builder-override` multimethod, where you can register overrides on a per bean class/key basis and call into the builder directly.

For example, instead of a method `void barField(String bar)` for the `barField` field on `BuiltBean`, `BuiltBean$Builder` uses `barFieldUnconventional`. This is handled like so:

```
(defmethod main/builder-override [BuiltBean :bar-field] [_ ^BuiltBean$Builder builder bar]
  (.barFieldUnconventional builder bar))

(main/def-builder-translation BuiltBean
                              BuiltBean$Builder
                              #{:bar-field}
                              {:builder-form (BuiltBean/builder)})
```

**Note that the `builder-override` defmethod *must* be performed *before* the evaluation of `def-builder-translation` for the relevant bean class.** Otherwise it will be ignored. This constraint is necessary for complete type hinting.

#### Options to def-builder-translation

The final, optional, argument to `def-builder-translation` is a map of  options:

| Key              | Description |
| ---              | ----------- |
| :builder-form    | Form to evaluate to an instance of the builder class. Default: `(BuilderClass.)`|
| :get-only-fields | Keys of read-only fields that are only read via the bean's getter in `Bean->map` and not set via the builder in `map->Bean`. Default: nil |
| :set-only-fields | Keys of write-only fields that are only set via the builder in `map->Bean` and not read by `Bean->map`. Default: nil|
| :build-method    | Symbol of method to call on builder to return a bean instance. Default: `.build` |

### With Spec

Often bean data models have contracts expressed via Javadoc documentation, such as valid ranges for the values of specific fields. It's useful to codify these contracts between your Clojure code and the wrapped Java API by writing [specs](https://clojure.org/about/spec), which enable you to validate, generate and reason about the data being exchanged. Only the `name` of keys are used to find bean field names, so they can be namespaced keywords with specs attached:

```
(require '[clojure.spec.alpha :as s])
(require '[clojure.spec.test.alpha :as stest])

(s/def ::foo-field #(= % 42))
(s/def ::test-map (s/keys :req [::foo-field]))

(bd/def-translation BasicBean #{::foo-field})

; during test, validate values before they're passed to the wrapped API
(s/fdef map->BasicBean :args (s/cat :test-map ::test-map))
(stest/instrument `map->BasicBean)

(map->BasicBean {::foo-field 41})
ExceptionInfo Call to #'user/map->BasicBean did not conform to spec:
In: [0 :user/foo-field] val: 41 fails spec: :user/foo-field at: [:args :test-map :user/foo-field] predicate: (= % 42)
  clojure.core/ex-info (core.clj:4739)

(map->BasicBean {::foo-field 42})
=> #object[bean_dip.BasicBean 0x48f45aaf "BasicBean{fooField=42}"]
```

## Error Handling

If you specify a non-existent bean field with `*warn-on-reflection*` set to true, you'll receive a compiler warning:

```
(set! *warn-on-reflection* true)

(bd/def-translation BasicBean #{:bam})
Reflection warning, repl.clj:1:1 - reference to field getBam on bean_dip.BasicBean can't be resolved.
Reflection warning, repl.clj:1:1 - call to method setBam on bean_dip.BasicBean can't be resolved (no such method).
```

If you ignore this warning, you'll get a runtime error when attempting to use the translation:

```
(map->BasicBean {:bam "ack"})
IllegalArgumentException No matching method found: setBam for class bean_dip.BasicBean  clojure.lang.Reflector.invokeMatchingMethod (Reflector.java:53)
```

_If someone can show me how to enable `*warn-on-reflection*` within the closure of the macro expansion I'd be grateful!_

## How it Works

Bean-dip simply generates the type annotated invocations of getters/setters you'd write in a manual translation layer, with a dash of naming conventions and extension hooks sprinkled on top. Here's a logical expansion of a `def-translation` form for illustration:

```
(magic-macroexpand '(def-translation BasicBean #{:foo-field}))
=>
(do
  (extend
    BasicBean
    bean-dip.core/TranslatableToMap
    {:bean->map (fn [^BasicBean bean]
                  (hash-map
                    :foo-field (bean-dip.core/resolve-map-value :foo-field
                                                                (.getFooField bean))))})
  [(defn BasicBean->map [bean]
     (bean-dip.core/bean->map bean))
   (defn map->BasicBean ^BasicBean [value-map]
     (doto
       (new BasicBean)
       (.setFooField (bean-dip.core/->bean-val :foo-field
                                               (:foo-field value-map)))))])
```

