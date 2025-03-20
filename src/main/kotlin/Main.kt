package com.nightlynexus.shapejudge

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get

class ShapeDrawingGame {
  private lateinit var canvas: HTMLCanvasElement
  private lateinit var ctx: CanvasRenderingContext2D
  private lateinit var clearButton: HTMLButtonElement
  private lateinit var submitButton: HTMLButtonElement
  private lateinit var shapeNameElement: HTMLSpanElement
  private lateinit var scoreElement: HTMLDivElement
  private lateinit var resultElement: HTMLDivElement
  private lateinit var shapeSelectorElement: HTMLElement
  private var isDrawing = false
  private val points = mutableListOf<Point>()
  private var currentShape: Shape? = null
  private val shapes = listOf(
    Shape.SQUARE,
    Shape.CIRCLE,
    Shape.TRIANGLE,
    Shape.PENTAGON,
    Shape.HEXAGON,
    Shape.SEPTAGON,
    Shape.OCTAGON
  )

  fun init() {
    // Initialize elements
    canvas = document.getElementById("canvas") as HTMLCanvasElement
    ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    clearButton = document.getElementById("clearButton") as HTMLButtonElement
    submitButton = document.getElementById("submitButton") as HTMLButtonElement
    shapeNameElement = document.getElementById("shapeName") as HTMLSpanElement
    scoreElement = document.getElementById("score") as HTMLDivElement
    resultElement = document.getElementById("result") as HTMLDivElement
    shapeSelectorElement = document.getElementById("shapeSelector") as HTMLElement

    // Set up event listeners
    canvas.addEventListener("mousedown", ::startDrawing)
    canvas.addEventListener("mousemove", ::draw)
    canvas.addEventListener("mouseup", { stopDrawing() })
    canvas.addEventListener("mouseout", { stopDrawing() })

    // Touch support
    canvas.addEventListener("touchstart", ::handleTouchStart)
    canvas.addEventListener("touchmove", ::handleTouchMove)
    canvas.addEventListener("touchend", { stopDrawing() })

    clearButton.addEventListener("click", { clearCanvas() })
    submitButton.addEventListener("click", { evaluateDrawing() })

    // Setup shape selector
    setupShapeSelector()
  }

  private fun setupShapeSelector() {
    val options = shapeSelectorElement.querySelectorAll(".shape-option")

    for (i in 0 until options.length) {
      val option = options[i] as HTMLElement
      option.addEventListener("click", {
        // Remove selected class from all options
        for (j in 0 until options.length) {
          (options[j] as HTMLElement).classList.remove("selected")
        }

        // Add selected class to clicked option
        option.classList.add("selected")

        // Set current shape
        val shapeName = option.getAttribute("data-shape") ?: "SQUARE"
        currentShape = Shape.valueOf(shapeName)
        shapeNameElement.textContent = currentShape!!.displayName

        // Enable submit button
        submitButton.disabled = false

        // Clear canvas
        clearCanvas()
      })
    }
  }

  private fun handleTouchStart(event: Event) {
    event as TouchEvent
    event.preventDefault()
    val touch = event.touches[0]!!
    val rect = canvas.getBoundingClientRect()
    val x = touch.clientX - rect.left
    val y = touch.clientY - rect.top
    isDrawing = true
    points.clear()
    points.add(Point(x, y))
    ctx.beginPath()
    ctx.moveTo(x, y)
  }

  private fun handleTouchMove(event: Event) {
    event as TouchEvent
    event.preventDefault()
    if (!isDrawing) return

    val touch = event.touches[0]!!
    val rect = canvas.getBoundingClientRect()
    val x = touch.clientX - rect.left
    val y = touch.clientY - rect.top

    ctx.lineTo(x, y)
    ctx.stroke()
    points.add(Point(x, y))
  }

  private fun startDrawing(event: Event) {
    event as MouseEvent
    val rect = canvas.getBoundingClientRect()
    val x = event.clientX - rect.left
    val y = event.clientY - rect.top
    isDrawing = true
    points.clear()
    ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
    scoreElement.textContent = ""
    resultElement.style.display = "none"
    points.add(Point(x, y))
    ctx.beginPath()
    ctx.moveTo(x, y)
  }

  private fun draw(event: Event) {
    event as MouseEvent
    if (!isDrawing) return

    val rect = canvas.getBoundingClientRect()
    val x = event.clientX - rect.left
    val y = event.clientY - rect.top

    ctx.lineTo(x, y)
    ctx.stroke()
    points.add(Point(x, y))
  }

  private fun stopDrawing() {
    if (isDrawing) {
      isDrawing = false

      // If the drawing is a closed shape, connect the first and last points
      if (points.size > 2) {
        val first = points.first()
        val last = points.last()
        val distance = sqrt((last.x - first.x).pow(2) + (last.y - first.y).pow(2))

        // If endpoints are close enough, connect them
        if (distance < 20) {
          ctx.lineTo(first.x, first.y)
          ctx.stroke()
          points.add(points.first()) // Add the first point again to close the shape
        }
      }
    }
  }

  private fun clearCanvas() {
    ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
    points.clear()
    scoreElement.textContent = ""
    resultElement.style.display = "none"
  }

  private fun evaluateDrawing() {
    if (points.size < 3) {
      scoreElement.textContent = "Please draw a shape first"
      return
    }

    if (currentShape == null) {
      scoreElement.textContent = "Please select a shape first"
      return
    }

    val score = calculateScore()
    val percentage = (score * 100).roundToInt()

    scoreElement.textContent = "Score: ${percentage}%"
    displayResult(percentage)

    // Draw the ideal shape in a lighter color
    drawIdealShape()
  }

  private fun drawIdealShape() {
    // Find the center of the drawn shape
    val center = findCenter(points)

    // Determine average radius
    val avgRadius = points.map {
      sqrt((it.x - center.x).pow(2) + (it.y - center.y).pow(2))
    }.average()

    ctx.save()
    ctx.strokeStyle = "rgba(0, 200, 0, 0.3)"
    ctx.lineWidth = 2.0

    when (currentShape) {
      Shape.CIRCLE -> {
        ctx.beginPath()
        ctx.arc(center.x, center.y, avgRadius, 0.0, 2 * PI)
        ctx.stroke()
      }

      Shape.SQUARE -> {
        // Draw square aligned with axes
        val halfSize = avgRadius * 0.8 // Adjust size to match user's drawing better
        ctx.beginPath()
        ctx.moveTo(center.x - halfSize, center.y - halfSize) // Top-left
        ctx.lineTo(center.x + halfSize, center.y - halfSize) // Top-right
        ctx.lineTo(center.x + halfSize, center.y + halfSize) // Bottom-right
        ctx.lineTo(center.x - halfSize, center.y + halfSize) // Bottom-left
        ctx.closePath()
        ctx.stroke()
      }

      else -> {
        val numVertices = when (currentShape) {
          Shape.TRIANGLE -> 3
          Shape.PENTAGON -> 5
          Shape.HEXAGON -> 6
          Shape.SEPTAGON -> 7
          Shape.OCTAGON -> 8
          else -> 4
        }

        // For polygons other than square, we use the circular distribution
        ctx.beginPath()
        for (i in 0 until numVertices) {
          // For triangle, start at the top (not left)
          val angleOffset = if (currentShape == Shape.TRIANGLE) -PI / 2 else 0.0
          val angle = (i * 2 * PI / numVertices) + angleOffset

          val x = center.x + avgRadius * cos(angle)
          val y = center.y + avgRadius * sin(angle)

          if (i == 0) {
            ctx.moveTo(x, y)
          } else {
            ctx.lineTo(x, y)
          }
        }
        ctx.closePath()
        ctx.stroke()
      }
    }

    ctx.restore()
  }

  private fun calculateScore(): Double {
    if (points.isEmpty()) return 0.0

    // Find the center of the drawn shape
    val center = findCenter(points)

    return when (currentShape) {
      Shape.CIRCLE -> evaluateCircle(center)
      Shape.SQUARE -> evaluateSquare(center)
      Shape.TRIANGLE -> evaluatePolygon(center, 3)
      Shape.PENTAGON -> evaluatePolygon(center, 5)
      Shape.HEXAGON -> evaluatePolygon(center, 6)
      Shape.SEPTAGON -> evaluatePolygon(center, 7)
      Shape.OCTAGON -> evaluatePolygon(center, 8)
      else -> 0.0
    }
  }

  private fun evaluateCircle(center: Point): Double {
    // For a circle, all points should be equidistant from the center
    val distances = points.map {
      sqrt((it.x - center.x).pow(2) + (it.y - center.y).pow(2))
    }

    val avgRadius = distances.average()
    val deviation = distances.map { abs(it - avgRadius) / avgRadius }.average()

    // Lower deviation means better circle
    return max(0.0, 1.0 - (deviation * 3.0))
  }

  private fun evaluateSquare(center: Point): Double {
    // For a square, we evaluate:
    // 1. Corners should be roughly 90 degrees
    // 2. Sides should be roughly equal length

    // Simplify to 4 points (approximate corners)
    val corners = approximateCorners(points, 4)

    // Calculate angle score
    val angleScore = corners.indices.map { i ->
      val prev = corners[(i - 1 + corners.size) % corners.size]
      val curr = corners[i]
      val next = corners[(i + 1) % corners.size]

      val angle = calculateAngle(prev, curr, next)
      // How close to 90 degrees (PI/2)
      1.0 - min(1.0, abs(angle - PI / 2) / (PI / 2))
    }.average()

    // Calculate side length score
    val sides = corners.indices.map { i ->
      val curr = corners[i]
      val next = corners[(i + 1) % corners.size]

      sqrt((next.x - curr.x).pow(2) + (next.y - curr.y).pow(2))
    }

    val avgSide = sides.average()
    val sideScore = 1.0 - sides.map { abs(it - avgSide) / avgSide }.average()

    return (angleScore * 0.7) + (sideScore * 0.3)
  }

  private fun evaluatePolygon(center: Point, numVertices: Int): Double {
    // For regular polygons, we evaluate:
    // 1. All vertices should be equidistant from center
    // 2. The angles between adjacent vertices should be equal

    // Simplify to n points (approximate vertices)
    val vertices = approximateCorners(points, numVertices)

    // Calculate distance score
    val distances = vertices.map {
      sqrt((it.x - center.x).pow(2) + (it.y - center.y).pow(2))
    }

    val avgRadius = distances.average()
    val distanceScore = 1.0 - min(1.0, distances.map { abs(it - avgRadius) / avgRadius }.average())

    // Calculate angle score
    val angles = vertices.indices.map { i ->
      val curr = vertices[i]
      val next = vertices[(i + 1) % vertices.size]

      val angle1 = atan2(curr.y - center.y, curr.x - center.x)
      val angle2 = atan2(next.y - center.y, next.x - center.x)

      var diff = (angle2 - angle1)
      // Normalize the angle difference
      while (diff < 0) diff += 2 * PI
      while (diff > 2 * PI) diff -= 2 * PI

      diff
    }

    val expectedAngle = 2 * PI / numVertices
    val angleScore =
      1.0 - min(1.0, angles.map { abs(it - expectedAngle) / expectedAngle }.average())

    return (distanceScore * 0.4) + (angleScore * 0.6)
  }

  private fun approximateCorners(points: List<Point>, numCorners: Int): List<Point> {
    if (points.size <= numCorners) return points

    // Use the Ramer-Douglas-Peucker algorithm for simplification
    val significantPoints = mutableListOf<Point>()

    // First, find points with highest curvature
    val curvatures = points.indices.map { i ->
      if (i == 0 || i == points.size - 1) {
        // Handle endpoints
        0.0
      } else {
        val prev = points[i - 1]
        val curr = points[i]
        val next = points[i + 1]

        // Calculate angle as a measure of curvature
        val angle = calculateAngle(prev, curr, next)
        abs(PI - angle) // Higher value means sharper turn
      }
    }

    // Sort points by curvature and take the top numCorners
    val cornerIndices = curvatures.withIndex()
      .sortedByDescending { it.value }
      .take(numCorners)
      .map { it.index }
      .sorted()

    // Return the corner points
    return cornerIndices.map { points[it] }
  }

  private fun calculateAngle(p1: Point, p2: Point, p3: Point): Double {
    // Calculate angle between three points with p2 as the vertex
    val v1x = p1.x - p2.x
    val v1y = p1.y - p2.y
    val v2x = p3.x - p2.x
    val v2y = p3.y - p2.y

    val mag1 = sqrt(v1x * v1x + v1y * v1y)
    val mag2 = sqrt(v2x * v2x + v2y * v2y)

    // Check for zero-length vectors
    if (mag1 < 1e-10 || mag2 < 1e-10) {
      return PI // Default to 180 degrees if vectors are too small
    }

    val dotProduct = v1x * v2x + v1y * v2y
    var cosine = dotProduct / (mag1 * mag2)

    // Clamp the value to valid arccos range to avoid NaN
    cosine = max(-1.0, min(1.0, cosine))

    return acos(cosine)
  }

  private fun findCenter(points: List<Point>): Point {
    val avgX = points.map { it.x }.average()
    val avgY = points.map { it.y }.average()
    return Point(avgX, avgY)
  }

  private fun displayResult(score: Int) {
    resultElement.className = "result"
    resultElement.style.display = "block"

    when {
      score >= 85 -> {
        resultElement.classList.add("good")
        resultElement.textContent = "Excellent! Your drawing is very accurate!"
      }

      score >= 60 -> {
        resultElement.classList.add("average")
        resultElement.textContent = "Good effort! Keep practicing!"
      }

      else -> {
        resultElement.classList.add("poor")
        resultElement.textContent = "Try again! Drawing shapes takes practice."
      }
    }
  }
}

data class Point(val x: Double, val y: Double)
enum class Shape(val displayName: String) {
  SQUARE("Square"),
  CIRCLE("Circle"),
  TRIANGLE("Equilateral Triangle"),
  PENTAGON("Pentagon"),
  HEXAGON("Hexagon"),
  SEPTAGON("Septagon"),
  OCTAGON("Octagon")
}

fun main() {
  window.onload = {
    val game = ShapeDrawingGame()
    game.init()
  }
}
