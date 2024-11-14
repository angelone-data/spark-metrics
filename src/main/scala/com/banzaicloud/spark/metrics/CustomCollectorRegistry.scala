package com.banzaicloud.spark.metrics

import io.prometheus.client.{Collector, CollectorRegistry}
import org.apache.spark.internal.Logging

import java.util.Collections
import java.{lang, util}
import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

// Supports
// 1.Configurable metric filtering via name
// 2.Deduplication of metrics
class CustomCollectorRegistry(val allowedMetricsString: Option[String],
                              parent: CollectorRegistry = CollectorRegistry.defaultRegistry)
  extends CollectorRegistry with Logging {

  private type MetricsEnum = util.Enumeration[Collector.MetricFamilySamples]
  private val allowedMetricSet: Set[String] =
    if (allowedMetricsString.isEmpty) Set() else allowedMetricsString.get.split(",").map(_.trim).toSet

  // Helper class to wrap a List as an Enumeration
  private class ListEnumeration[T](list: List[T]) extends util.Enumeration[T] {
    private val elements = list.iterator

    override def hasMoreElements: Boolean = elements.hasNext

    override def nextElement: T = elements.next()
  }

  override def register(m: Collector): Unit = {
    logInfo("Trace1")
    Try(parent.register(m)) match {
      case Failure(ex) if ex.getMessage.startsWith("Collector already registered that provides name:") =>
      // TODO: find a more robust solution for checking if there is already a collector registered for a specific metric
      case Failure(ex) => throw ex
      case _ =>
    }
  }

  override def unregister(m: Collector): Unit = parent.unregister(m)

  override def clear(): Unit = parent.clear()

  override def getSampleValue(name: String, labelNames: Array[String], labelValues: Array[String]): lang.Double = {
    parent.getSampleValue(name, labelNames, labelValues)
  }

  override def getSampleValue(name: String): lang.Double = parent.getSampleValue(name)

  override def metricFamilySamples(): MetricsEnum = {
    logInfo("Trace2")
    val filteredSamples = filterMetrics(parent.metricFamilySamples())
    deduplicate(filteredSamples)
  }

  override def filteredMetricFamilySamples(includedNames: util.Set[String]): MetricsEnum = {
    val filteredSamples = filterMetrics(parent.filteredMetricFamilySamples(includedNames))
    deduplicate(filteredSamples)
  }

  // 1. allowedMetricSet empty or * means everything is allowed.
  // 2. Else, metrics with exact name match from allowedMetricSet are allowed
  private def filterMetrics(allSamples: MetricsEnum): MetricsEnum = {
    allSamples.asScala.foreach { metric =>
      println(s"Metric: ${metric.name}, Type: ${metric.`type`}, ${metric.unit}")
    }
    if (allowedMetricSet.isEmpty || (allowedMetricSet.size == 1 && allowedMetricSet.contains("*"))) {
      return allSamples
    }

    val filteredSamples = allSamples.asScala.toSeq.filter(sample => allowedMetricSet.contains(sample.name))
    new ListEnumeration(filteredSamples.toList)
  }

  private def deduplicate(source: MetricsEnum): MetricsEnum = {
    val metrics = source.asScala.toSeq
    val deduplicated = metrics
      .groupBy(f => (f.name, f.`type`))
      .flatMap {
        case (_, single) if single.lengthCompare(2) < 0 =>

          single
        case ((name, metricType), duplicates) =>
          logDebug(s"Found ${duplicates.length} metrics with the same name '${name}' and type ${metricType}")
          duplicates.lastOption
      }
      .toList
      .asJava
    Collections.enumeration(deduplicated)
  }
}
