(defproject uwcpdx/bean-dip "0.7.1"
  :description "Bidirectional translation between maps and Java beans that's declarative and reflection-free"
  :url "https://github.com/uwcpdx/bean-dip"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :java-source-paths ["java"]
  :deploy-repositories {"clojars" {:url           "https://clojars.org/repo"
                                   :username      :env/CLOJARS_USERNAME
                                   :password      :env/CLOJARS_PASSWORD
                                   :sign-releases false}})

