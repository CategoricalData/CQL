;; A project to convert AQL files into various forms.

(def project 'babeloff/aql)
(def version "2017.11.01-SNAPSHOT")

(set-env!
    :source-paths #{"src/antlr4"}
    :dependencies '[[org.clojure/clojure "RELEASE"]
                    [org.clojure/spec.alpha "0.1.143"]
                    [boot/core "RELEASE" :scope "test"]
                    [babeloff/boot-antlr4 "2018.06.07-SNAPSHOT"]
                    [babeloff/boot-antlr4-parser "2017.10.31"]
                    [org.antlr/antlr4 "4.7"]
                    [clj-jgit "0.8.10"]
                    [byte-streams "0.2.3"]
                    [me.raynes/fs "1.4.6"]

                    [org.apache.commons/commons-rdf-jena "0.5.0"]])

(task-options!
 pom {:project     project
      :version     version
      :packaging   "jar"
      :description "help in generating the java based parser for AQL"
      :url         "http://example/FIXME"
      :scm         {:url "https://github.com/CategoricalData/fql"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}
      :developers  {"Fred Eisele" ""}})


;; (import '(org.antlr.v4.gui TestRig))
(require
  '(boot [core :as boot :refer [deftask]]
         [util :as util]
         [task-helpers :as helper])
  '(babeloff
    [boot-antlr4 :as antlr]
    [boot-antlr4-parser :as coach])
  '(clojure.spec [alpha :as s])
  '(clojure.java [io :as io]))

(import
  '(java.nio.file Path Paths Files
                  FileVisitOption LinkOption))

(deftask store
  []
  (comp 
   (sift :invert true :include #{ #".*\.(java|class)"})
   (target :dir #{"target"})))

(deftask build
  [s show bool "show the arguments"]
  (comp
    (antlr/generate
      :grammar "AqlLexerRules.g4"
      :package "catdata.aql.grammar")
    (antlr/generate
      :grammar "AqlParser.g4"
      :package "catdata.aql.grammar"
      :visitor true
      :listener true)
    (javac)))

(def no-path-extensions (into-array String []))
(def no-file-visit-options (into-array FileVisitOption []))
(def no-link-options (into-array LinkOption []))

(defn get-file-name [f] (.getFileName f))
(defn matches [gm f] (.matches gm (get-file-name f)))
(defn get-path-matcher [pattern]
  (.getPathMatcher (java.nio.file.FileSystems/getDefault) pattern))

(defn get-file-paths
  [sample-dir file-glob-pattern]
  (let [input-dir-str sample-dir
        input-dir (Paths/get input-dir-str no-path-extensions)
        input-raw-s
          (let [children (Files/walk input-dir 1 no-file-visit-options)]
            (iterator-seq (.iterator children)))
        gm (get-path-matcher file-glob-pattern)
        input-file-s (filter #(matches gm %1) input-raw-s)]
    input-file-s))

;; (into [] (map #(.getFileName %)) (file/walk path 1))
(deftask parse-examples
  [p example-dir VAL str "provide a path to the examples"]
  (let [input-files (mapv str (get-file-paths example-dir "glob:*.{aql}"))]
    (util/info "input files : %s\n" input-files)
    (comp
      (antlr/exercise
        :parser "catdata.aql.grammar.AqlParser"
        :lexer "catdata.aql.grammar.AqlLexerRules"
        :start-rule "file"
        :tree true
        :edn true
        :rdf :jena
        :postscript true
        :tokens true
        :show true
        :input input-files))))

(deftask parse-grammar
  []
  (coach/exercise
    :start-rule "grammarSpec"

    :tree false
    :edn true
    :rdf :jena
    :postscript false
    :tokens false

    :input ["src/antlr4/LexBasic.g4"
            "src/antlr4/AqlLexerRules.g4"
            "src/antlr4/AqlParser.g4"
            "src/antlr4/AqlComment.g4"
            "src/antlr4/AqlTypeside.g4"
            "src/antlr4/AqlGraph.g4"
            "src/antlr4/AqlOptions.g4"
            "src/antlr4/AqlInstance.g4"
            "src/antlr4/AqlMapping.g4"
            "src/antlr4/AqlCommand.g4"
            "src/antlr4/AqlQuery.g4"
            "src/antlr4/AqlSchema.g4"
            "src/antlr4/AqlSchemaColimit.g4"
            "src/antlr4/AqlTransform.g4"]))


(deftask my-repl
  [s show bool "show the arguments"]
  (comp (repl) (build)))

(defn poll
  "https://adzerk.com/blog/2017/02/faster-clojure-metadevelopment-with-boot/"
  [task]
  (let [f (java.io.File. "build.boot")]
    (loop [mtime (.lastModified f)]
      (let [new-mtime (.lastModified f)]
        (when (> new-mtime mtime)
          (load-file "build.boot")
          (boot (task)))
        (Thread/sleep 1000)
        (recur new-mtime)))))


(deftask dev
  []
  (comp
    (build)
    (parse-grammar)
    (parse-examples :example-dir "../../../../resources/examples/aql")
    (parse-examples :example-dir "../../../../resources/examples/aql_extra")
    ;; (show :fileset true)
    (store)))

(deftask live [] (comp (watch) (dev)))

(deftask promote
  []
  (comp
    (build)
    (parse-grammar)
    (parse-examples :example-dir "../../../../resources/examples/aql")
    (parse-examples :example-dir "../../../../resources/examples/aql_extra")
    (sift :include #{ #".*\.java"})
    (target :dir #{"../../../../gen/catdata/aql/grammar"})))

