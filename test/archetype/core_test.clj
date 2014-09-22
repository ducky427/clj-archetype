(ns archetype.core-test)


(def valid-md5-hash "28a4b1c1dcd574bfb527e81426bc1e09")
(def valid-md5-hash2 "7a0c47442e43f063ae079ec5c0362562")
(def valid-md5-hash3 "9da96a72d9bc49463f6999cbfeea864c")
(def valid-email    "rohit.aggarwal@gmail.com")
(def not-found-email "not@found.com")

(def valid-url "http://en.wikipedia.org/wiki/Neo4j")
(def valid-url2 "http://en.wikipedia.org/wiki/Nutella")
(def valid-url3 "http://en.wikipedia.org/wiki/Metiallica")
(def invalid-wiki-url "http://en.notwikipedia.org/wiki/Neo4j")

(def valid-identity-hash  {"identity" valid-md5-hash})
(def valid-identity-pages [valid-url valid-url2])
(def valid-identity-knows [valid-md5-hash2 valid-md5-hash3])

(def valid-page-hash {"url" valid-url})

(def valid-page-links [valid-url2 valid-url3])
