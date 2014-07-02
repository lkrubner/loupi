(defproject loupi "0.1"
  :description "loupi is the Launch Open User Persistence Integration -- it provides endpoints to our front Javascript to enable all CRUD operations via Ajax."
  :url "http://launchopen.com/"
  :license {:name "Copyright Launch Open 2014"
            :url "http://www.launchopen.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [dire "0.5.1"]
                 [ring "1.2.1"]
                 [clj-time "0.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]
                 [com.novemberain/monger "2.0.0"]
                 [com.taoensso/timbre "2.7.1"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [formative "0.8.7"]
                 [lamina "0.5.0"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/core.incubator "0.1.3"]
                 [liberator "0.11.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.0"]]}}
  :repositories [["central-proxy" "https://repository.sonatype.org/content/repositories/centralm1/"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :disable-implicit-clean true
  :warn-on-reflection true
  :main loupi.core
  :jvm-opts ["-Xms512m" "-Xmx1020m" "-XX:-UseCompressedOops"])



