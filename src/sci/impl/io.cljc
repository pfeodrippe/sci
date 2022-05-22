(ns sci.impl.io
  {:no-doc true}
  (:refer-clojure :exclude [pr prn pr-str prn-str print print-str println
                            newline flush with-out-str with-in-str read-line
                            printf #?@(:cljs [string-print])
                            print-method])
  (:require #?(:cljs [goog.string])
            [sci.impl.records]
            [sci.impl.unrestrict :refer [*unrestricted*]]
            #?(:cljs [sci.impl.utils :as utils])
            [sci.impl.vars :as vars]
            [missing.stuff :refer [instance?]]))

#?(:cljd ()
   :clj
   (defmulti print-method (fn [x _writer]
                            (let [t (-> x meta :type)]
                              (if (and t
                                       (or (instance? sci.impl.records.SciRecord x)
                                           (keyword? t)))
                                t
                                (class x))))))

#?(:cljd ()
   :clj
   (defmethod print-method :default
     [x writer]
     (clojure.core/print-method x writer)))

#?(:cljd ()
   :clj (set! *warn-on-reflection* true))

(defn core-dynamic-var
  "create a dynamic var with clojure.core :ns meta"
  ([name] (core-dynamic-var name nil))
  ([name init-val] (vars/dynamic-var name init-val {:ns vars/clojure-core-ns})))

(def in (binding [*unrestricted* true]
          (doto (core-dynamic-var '*in*)
            (vars/unbind))))

(def out (binding [*unrestricted* true]
           (doto (core-dynamic-var '*out*)
             (vars/unbind))))

(def err (binding [*unrestricted* true]
           (doto (core-dynamic-var '*err*)
             (vars/unbind))))

#?(:cljs
   (def print-fn
     (binding [*unrestricted* true]
       (doto (core-dynamic-var '*print-fn*)
         (vars/unbind)))))

#?(:cljs
   (def print-err-fn
     (binding [*unrestricted* true]
       (doto (core-dynamic-var '*print-err-fn*)
         (vars/unbind)))))

;; TODO: CLJS print-fn-bodies

(def print-meta
  (core-dynamic-var '*print-meta* false))

(def print-length (core-dynamic-var '*print-length*))
(def print-level (core-dynamic-var '*print-level*))
(def print-namespace-maps (core-dynamic-var '*print-namespace-maps* true))
(def flush-on-newline (atom nil) #_(core-dynamic-var '*flush-on-newline* *flush-on-newline*))
(def print-readably (core-dynamic-var '*print-readably* *print-readably*))
(def print-dup-var (atom nil) #_(core-dynamic-var '*print-dup* *print-dup*))
#?(:cljs (def print-newline (core-dynamic-var '*print-newline* *print-newline*)))

#?(:cljs (defn string-print [x]
           (binding [*print-fn* @print-fn]
             (cljs.core/string-print x))) )

#?(:cljd ()
   :clj (defn pr-on
          {:private true
           :static true}
          [x w]
          (if *print-dup*
            (print-dup x w)
            (print-method x w))
          nil))

#?(:cljd (defn pr
           [& objs])
   :clj (defn pr
          ([] nil)
          ([x]
           (binding [#_ #_*print-length* @print-length
                     #_ #_*print-level* @print-level
                     #_ #_*print-meta* @print-meta
                     #_ #_*print-namespace-maps* @print-namespace-maps
                     *print-readably* @print-readably
                     *print-dup* @print-dup-var]
             (pr-on x @out)))
          ([x & more]
           (pr x)
           #_(. ^java.io.Writer @out (append \space))
           (if-let [nmore (next more)]
             (recur (first more) nmore)
             (apply pr more))))
   :cljs (defn pr
           [& objs]
           (binding [*print-fn* @print-fn
                     *print-length* @print-length
                     *print-level* @print-level
                     *print-meta* @print-meta
                     *print-namespace-maps* @print-namespace-maps
                     *print-readably* @print-readably
                     *print-newline* @print-newline
                     *print-dup* @print-dup-var]
             (apply cljs.core/pr objs))))

#?(:cljd (defn flush []                 ;stub
           nil)
   :clj
   (defn flush
     []
     (. ^java.io.Writer @out (flush))
     nil)
   :cljs (defn flush []                 ;stub
           nil))

#?(:cljs (declare println))

#?(:cljd (defn newline
           []
           #_(binding [*print-fn* @print-fn]
             #_(cljs.core/newline)))
   :clj (defn newline
          []
          (. ^java.io.Writer @out (append ^String @#'clojure.core/system-newline))
          nil)
   :cljs (defn newline
           []
           (binding [*print-fn* @print-fn]
             (cljs.core/newline))))

#?(:cljd
   (defn pr-str
     [& xs])
   :clj
   (defn pr-str
     "pr to a string, returning it"
     [& xs]
     (let [sw (java.io.StringWriter.)]
       (vars/with-bindings {out sw}
         (apply pr xs))
       (str sw)))
   :cljs
   (defn pr-str
     "pr to a string, returning it"
     [& objs]
     (binding [*print-length* @print-length
               *print-level* @print-level
               *print-meta* @print-meta
               *print-namespace-maps* @print-namespace-maps
               *print-readably* @print-readably
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/pr-str objs))))

#?(:clj
   (defn prn
     [& more]
     (apply pr more)
     (newline)
     (when @flush-on-newline
       (flush)))
   :cljs
   (defn prn
     [& objs]
     (binding [*print-fn* @print-fn
               *print-length* @print-length
               *print-level* @print-level
               *print-meta* @print-meta
               *print-namespace-maps* @print-namespace-maps
               *print-readably* @print-readably
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/prn objs))))

#?(:cljd
   (defn prn-str
     "prn to a string, returning it"
     [& xs])
   :clj
   (defn prn-str
     "prn to a string, returning it"
     [& xs]
     (let [sw (java.io.StringWriter.)]
       (vars/with-bindings {out sw}
         (apply prn xs))
       (str sw)))
   :cljs
   (defn prn-str
     "prn to a string, returning it"
     [& objs]
     (binding [*print-length* @print-length
               *print-level* @print-level
               *print-meta* @print-meta
               *print-namespace-maps* @print-namespace-maps
               *print-readably* @print-readably
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/prn-str objs))))

#?(:clj
   (defn print
     [& more]
     (vars/with-bindings {print-readably nil}
       (apply pr more)))
   :cljs
   (defn print
     [& objs]
     (binding [*print-fn* @print-fn
               *print-length* @print-length
               *print-level* @print-level
               *print-namespace-maps* @print-namespace-maps
               *print-readably* nil
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/print objs))))

#?(:cljd
   (defn print-str
     "print to a string, returning it"
     [& xs])
   :clj
   (defn print-str
     "print to a string, returning it"
     [& xs]
     (let [sw (java.io.StringWriter.)]
       (vars/with-bindings {out sw}
         (apply print xs))
       (str sw)))
   :cljs
   (defn print-str
     "print to a string, returning it"
     [& objs]
     (binding [*print-length* @print-length
               *print-level* @print-level
               *print-meta* @print-meta
               *print-namespace-maps* @print-namespace-maps
               *print-readably* @print-readably
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/print-str objs))))

#?(:clj
   (defn println
     [& more]
     (vars/with-bindings {print-readably nil}
       (apply prn more)))
   :cljs
   (defn println
     [& objs]
     (binding [*print-fn* @print-fn
               *print-length* @print-length
               *print-level* @print-level
               *print-meta* @print-meta
               *print-namespace-maps* @print-namespace-maps
               *print-readably* @print-readably
               *print-newline* @print-newline
               *print-dup* @print-dup-var]
       (apply cljs.core/println objs))))

#?(:cljd
   (defn printf
     [fmt & args]
     #_(print (apply format fmt args)))
   :clj
   (defn printf
     [fmt & args]
     (print (apply format fmt args))))

(defn with-out-str
  [_ _ & body]
  `(let [s# (new #?(:clj java.io.StringWriter
                   :cljs goog.string.StringBuffer))]
     #?(:clj
        (binding [*out* s#]
          ~@body
          (str s#))
        :cljs
        (binding [*print-newline* true
                  *print-fn* (fn [x#]
                               (. s# ~utils/allowed-append x#))]
          ~@body
          (str s#)))))

#?(:clj
   (defn with-in-str
     [_ _ s & body]
     `(with-open [s# (-> (java.io.StringReader. ~s) clojure.lang.LineNumberingPushbackReader.)]
        (binding [*in* s#]
          ~@body))))

#?(:cljd
   (defn read-line
     [])
   :clj
   (defn read-line
     []
     (if (instance? clojure.lang.LineNumberingPushbackReader @in)
       (.readLine ^clojure.lang.LineNumberingPushbackReader @in)
       (.readLine ^java.io.BufferedReader @in))))
