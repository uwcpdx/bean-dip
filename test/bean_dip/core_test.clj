(ns bean-dip.core-test
  (:require [clojure.test :as test]
            [bean-dip.core :as main])
  (:import [bean_dip BasicBean ParentBean BuiltBean BuiltBean$Builder BigBean]))

(set! *warn-on-reflection* true)

(defmethod main/->map-val :foo [_ value]
  (str value))

(defmethod main/->bean-val :foo [_ value]
  (Long/parseLong value))

(main/def-translation ParentBean #{:children})
(main/def-translation BasicBean #{[:foo-field :foo]})

(defmethod main/->bean-val :children [_ value]
  (mapv map->BasicBean value))

(def basic-map {:foo "42"})
(def basic-bean (doto (BasicBean.)
                  (.setFooField 42)))

(def parent-map
  {:children [basic-map]})

(def parent-bean
  (doto (ParentBean.)
    (.setChildren [basic-bean])))

(test/deftest parent-child
  (test/testing "ParentBean->map"
    (test/is (= parent-map (ParentBean->map parent-bean))))
  (test/testing "map->ParentBean"
    (test/is (= parent-bean (map->ParentBean parent-map))))
  (test/testing "omit keys with null values"
    (test/is (= {} (ParentBean->map (ParentBean.))))))

(defmethod main/builder-override [BuiltBean :bar-field] [_ ^BuiltBean$Builder builder bar]
  (.barFieldUnconventional builder bar))

(main/def-builder-translation BuiltBean
                              BuiltBean$Builder
                              #{[:foo-field :foo]
                                :bar-field
                                :some-condition?
                                :read-only-field}
                              {:builder-form    (BuiltBean/builder)
                               :get-only-fields #{:read-only-field}})

(def test-map
  {:foo             "42"
   :bar-field       "hello"
   :some-condition? true
   :read-only-field "READ ONLY"})

(def built-bean (BuiltBean. 42 "hello" true))

(test/deftest built-bean-test
  (test/is (= built-bean (.build (map->BuiltBean$Builder test-map)))))

(def big-bean
  (BigBean. "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a" "a"))

(def big-bean-map
  {:field1  "a"
   :field2  "a"
   :field3  "a"
   :field4  "a"
   :field5  "a"
   :field6  "a"
   :field7  "a"
   :field8  "a"
   :field9  "a"
   :field10 "a"
   :field11 "a"
   :field12 "a"
   :field13 "a"
   :field14 "a"
   :field15 "a"})

(main/def-translation BigBean #{:field1
                                :field2
                                :field3
                                :field4
                                :field5
                                :field6
                                :field7
                                :field8
                                :field9
                                :field10
                                :field11
                                :field12
                                :field13
                                :field14
                                :field15})

(test/deftest big-bean-test
  (test/is (= (BigBean->map big-bean) big-bean-map)))

