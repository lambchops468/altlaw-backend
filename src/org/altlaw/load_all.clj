(ns org.altlaw.load-all
  (:require 
   org.altlaw.jobs.web.assignids
   org.altlaw.jobs.web.crawl
   org.altlaw.jobs.web.files
   org.altlaw.jobs.web.merge
   org.altlaw.jobs.web.scrape
   org.altlaw.jobs.web.seed
   org.altlaw.jobs.pre.ohm1
   org.altlaw.jobs.pre.profed
   org.altlaw.jobs.analyze.graph
   org.altlaw.jobs.analyze.cite_fields
   org.altlaw.jobs.analyze.merge
   org.altlaw.jobs.post.distindex
   org.altlaw.jobs.post.makecontent

   org.altlaw.test.run
   org.altlaw.test.www
   org.altlaw.test.www.search

   org.altlaw.util.context
   org.altlaw.util.date
   org.altlaw.util.files
   org.altlaw.util.hadoop
   org.altlaw.util.hadoop.SeqFileToFiles
   org.altlaw.util.hadoop.SeqFileTextToFiles
   org.altlaw.util.log
   org.altlaw.util.lucene
   org.altlaw.util.markdown
   org.altlaw.util.solr
   org.altlaw.util.s3
   org.altlaw.util.zip

   org.altlaw.www.render
   org.altlaw.www.content-pages
   org.altlaw.www.application
   org.altlaw.www.server
))
