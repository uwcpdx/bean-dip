(defproject uwcpdx/bean-dip "0.7.4-SNAPSHOT"
  :description "Bidirectional translation between maps and Java beans that's declarative and reflection-free"
  :url "https://github.com/uwcpdx/bean-dip"
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/spec.alpha "0.1.143"]]}}
  :java-source-paths ["java"]
  :deploy-repositories {"clojars" {:url           "https://clojars.org/repo"
                                   :username      :env/CLOJARS_USERNAME
                                   :password      :env/CLOJARS_PASSWORD
                                   :sign-releases false}})

