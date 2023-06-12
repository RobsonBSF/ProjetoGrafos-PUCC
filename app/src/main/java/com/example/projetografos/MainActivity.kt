package com.example.projetografos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var graph: HashMap<String, HashMap<String, Long>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.calcular)

        btn.setOnClickListener{
            fetchBuildingDataFromFirestore()
        }
    }

    private fun fetchBuildingDataFromFirestore() {
        firestore.collection("predios")
            .get()
            .addOnSuccessListener { documents ->
                val buildings = HashMap<String, HashMap<String, Long>>()
                for (document in documents) {
                    val buildingName = document.getString("nome") ?: ""
                    buildings[buildingName] = HashMap()
                }
                graph = buildings

                fetchPathsDataFromFirestore()
            }
    }

    private fun fetchPathsDataFromFirestore() {
        firestore.collection("caminhos")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val sourceBuilding = document.getString("predio_origem") ?: ""
                    val destinationBuilding = document.getString("predio_destino") ?: ""
                    val travelTime = document.getLong("tempo") ?: 0

                    graph[sourceBuilding]?.put(destinationBuilding, travelTime)

                    if (graph[destinationBuilding] == null) {
                        graph[destinationBuilding] = HashMap()
                    }
                    graph[destinationBuilding]?.put(sourceBuilding, travelTime)
                }

                val origem = findViewById<EditText>(R.id.origem).text.toString()
                val destino = findViewById<EditText>(R.id.destino).text.toString()

                val shortestPath = dijkstra(origem, destino)
                displayShortestPath(shortestPath)
            }
    }

    private fun dijkstra(start: String, end: String): List<String> {
        val distances = HashMap<String, Long>()
        val previous = HashMap<String, String>()
        val unvisited = ArrayList<String>()

        for (building in graph.keys) {
            distances[building] = Long.MAX_VALUE
            unvisited.add(building)
        }
        distances[start] = 0

        while (unvisited.isNotEmpty()) {
            val currentBuilding = unvisited.minByOrNull { distances[it]!! } ?: break
            unvisited.remove(currentBuilding)

            for ((neighbor, distance) in graph[currentBuilding]!!) {
                val newDistance = distances[currentBuilding]!! + distance
                if (newDistance < distances[neighbor]!!) {
                    distances[neighbor] = newDistance
                    previous[neighbor] = currentBuilding
                }
            }
        }

        val shortestPath = ArrayList<String>()
        var currentBuilding = end
        while (currentBuilding != start) {
            shortestPath.add(0, currentBuilding)
            currentBuilding = previous[currentBuilding] ?: break
        }
        shortestPath.add(0, start)

        return shortestPath
    }

    private fun displayShortestPath(shortestPath: List<String>) {
        val resultText = StringBuilder()
        resultText.append("Caminho mais curto: ")
        for ((index, building) in shortestPath.withIndex()) {
            resultText.append(building)
            if (index != shortestPath.lastIndex) {
                resultText.append(" -> ")
            }
        }
        resultText.append("\n")

        val totalTime = calculateTotalTime(shortestPath)
        resultText.append("Tempo total: $totalTime minutos")

        val resultado = findViewById<TextView>(R.id.resultado)
        resultado.text = resultText.toString()
    }

    private fun calculateTotalTime(shortestPath: List<String>): Long {
        var totalTime: Long = 0
        for (i in 0 until shortestPath.lastIndex) {
            val currentBuilding = shortestPath[i]
            val nextBuilding = shortestPath[i + 1]
            totalTime += graph[currentBuilding]?.get(nextBuilding) ?: 0
        }
        return totalTime
    }
}
