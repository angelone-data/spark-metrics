package com.banzaicloud.spark.metrics

import io.prometheus.client.{Collector, CollectorRegistry}
import org.apache.spark.internal.Logging

import java.util.Collections
import java.{lang, util}
import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

class DeduplicatedCollectorRegistry(parent: CollectorRegistry = CollectorRegistry.defaultRegistry)
  extends CollectorRegistry with Logging {

  private type MetricsEnum = util.Enumeration[Collector.MetricFamilySamples]
  private val allowedMetricNames = Set("activeOpCount", "NamedExecutor_num_active_tasks", "NamedExecutor_num_pending_tasks", "jmx_GarbageCollector_LastGCInfo_memoryUsageAfterGc_used")

  // Helper class to wrap a List as an Enumeration
  private class ListEnumeration[T](list: List[T]) extends util.Enumeration[T] {
    private val elements = list.iterator

    override def hasMoreElements: Boolean = elements.hasNext

    override def nextElement: T = elements.next()
  }

  override def register(m: Collector): Unit = {
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
    val filteredSamples = filterMetrics(parent.metricFamilySamples())
    deduplicate(filteredSamples)
  }

  override def filteredMetricFamilySamples(includedNames: util.Set[String]): MetricsEnum = {
    val filteredSamples = filterMetrics(parent.filteredMetricFamilySamples(includedNames))
    deduplicate(filteredSamples)
  }

  // Method to filter metrics based on the custom logic
  private def filterMetrics(allSamples: MetricsEnum): MetricsEnum = {
    val filteredSamples = allSamples.asScala.toSeq.filter(
      {
        sample =>
          logInfo(sample.name)
          allowedMetricNames.contains(sample.name)
      }
    )
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
