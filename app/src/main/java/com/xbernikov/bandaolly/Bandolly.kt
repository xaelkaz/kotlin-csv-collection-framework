package com.xbernikov.bandaolly

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


data class TimeSeries(val points: List<Point>, val attr: Attr)

enum class Tolerance { CRITICAL, IMPORTANT, REGULAR }

data class Attr(val name: String, val tolerance: Tolerance)

data class Point(
    val serial: String,
    val date: LocalDateTime,
    val value: Double
)

fun createCsv(timeSeries: List<TimeSeries>): String {
    val distinctAttrs = timeSeries.distinctBy { it.attr }.map { it.attr }.sortedBy { it.name }
    val csvHeader = "date;serial;" + distinctAttrs.joinToString(";") { it.name } + "\n"

    data class PointWithAttr(val point: Point, val attr: Attr)

    val pointsWithAttrs = timeSeries.flatMap {
        it.points.map { point -> PointWithAttr(point, it.attr) }
    }
    /*
        TimeSeries(points=[Point(serial=HC11, date=2020-07-27T15:45, value=15.1), Point(serial=HC12, date=2020-07-27T15:35, value=15.05), Point(serial=HC13, date=2020-07-27T15:25, value=15.11), Point(serial=HC14, date=2020-07-27T15:15, value=15.08)], attr=Attr(name=AngleOfAttack, tolerance=CRITICAL))
        TimeSeries(points=[Point(serial=HC11, date=2020-07-27T15:45, value=0.68), Point(serial=HC12, date=2020-07-27T15:35, value=0.7), Point(serial=HC13, date=2020-07-27T15:25, value=0.69), Point(serial=HC14, date=2020-07-27T15:15, value=0.71)], attr=Attr(name=ChordLength, tolerance=IMPORTANT))
        TimeSeries(points=[Point(serial=HC11, date=2020-07-27T15:45, value=2201331.0), Point(serial=HC14, date=2020-07-27T15:15, value=7951688.0)], attr=Attr(name=PaintColor, tolerance=REGULAR))

        flatMap => List of 10
        PointWithAttr(point=Point(serial=HC11, date=2020-07-27T15:45, value=15.1), attr=Attr(name=AngleOfAttack, tolerance=CRITICAL))
        PointWithAttr(point=Point(serial=HC12, date=2020-07-27T15:35, value=15.05), attr=Attr(name=AngleOfAttack, tolerance=CRITICAL))
        PointWithAttr(point=Point(serial=HC11, date=2020-07-27T15:45, value=0.68), attr=Attr(name=ChordLength, tolerance=IMPORTANT))
     */

    // filtered to critical and important
    val importantPointsWithAttrs = timeSeries.filter {
        it.attr.tolerance == Tolerance.CRITICAL || it.attr.tolerance == Tolerance.IMPORTANT
    }.map { series ->
        series.points.map { point -> PointWithAttr(point, series.attr) }
    }.flatten()

    // Lista identica al flatmap, con la diferencia que tiene un filtro por tolerance

    val rows = importantPointsWithAttrs
        .groupBy { it.point.date }  // <1>
        .toSortedMap().map { (date, ptsWithAttrs1) ->                   // <3>
            ptsWithAttrs1
                .groupBy { it.point.serial }
                .map { (serial, ptsWithAttrs2) ->         // <4>
                    listOf(date.format(DateTimeFormatter.ISO_LOCAL_DATE), serial) +
                            distinctAttrs.map { attr ->
                                val value = ptsWithAttrs2.firstOrNull { it.attr == attr }
                                value?.point?.value?.toString() ?: ""
                            }
                }.joinToString(separator = "") {          // <5>
                    it.joinToString(separator = ";", postfix = "\n")
                }
        }.joinToString(separator = "")

    return csvHeader + rows
}