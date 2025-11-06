package com.proyecto.marvic.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class MaterialItem(
    val id: String = "",
    val codigo: String = "", // Código QR/Barras único
    val nombre: String = "",
    val descripcion: String = "", // Descripción detallada
    val categoria: String = "", // Categoría del material
    val cantidad: Int = 0,
    val unidadMedida: String = "unidades", // kg, litros, m2, etc.
    val ubicacion: String = "",
    val almacen: String = "", // ID del almacén
    val precioUnitario: Double = 0.0, // Precio por unidad
    val proveedorId: String = "", // ID del proveedor principal
    val stockMinimo: Int = 10, // Stock de seguridad
    val stockMaximo: Int = 1000, // Stock máximo
    val imagenUrl: String = "", // URL de la imagen
    val activo: Boolean = true, // Si está en uso
    val notas: String = "", // Notas adicionales
    val peso: Double = 0.0, // Peso en kg
    val dimensiones: String = "", // "20x30x40 cm"
    val fechaCreacion: Long = 0L,
    val fechaActualizacion: Long = 0L,
    val ultimaCompra: Long = 0L
)

interface InventoryRepository {
    suspend fun searchMaterials(query: String): Result<List<MaterialItem>>
    suspend fun searchByCode(code: String): Result<MaterialItem?>
    suspend fun getMaterialById(id: String): Result<MaterialItem?>
    suspend fun createMaterial(material: MaterialItem): Result<MaterialItem>
    suspend fun updateMaterial(material: MaterialItem): Result<MaterialItem>
    suspend fun deleteMaterial(materialId: String): Result<Unit>
    suspend fun registerMovement(materialId: String, delta: Int): Result<Unit>
    suspend fun totalStock(): Result<Int>
    suspend fun getMaterialsByCategory(category: String): Result<List<MaterialItem>>
    suspend fun getLowStockMaterials(): Result<List<MaterialItem>>
    suspend fun recentMovements(limit: Int = 50): Result<List<Movement>>
    suspend fun getMovementsInLast24Hours(): Result<List<Movement>>
}

data class Movement(
    val id: String = "",
    val materialId: String = "",
    val delta: Int = 0,
    val timestamp: Long = 0L,
    val userId: String? = null,
)

class FirestoreInventoryRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : InventoryRepository {
    private val collection get() = db.collection("materials")
    private val moves get() = db.collection("movements")

    override suspend fun searchMaterials(query: String): Result<List<MaterialItem>> = try {
        val snapshot = collection.whereGreaterThanOrEqualTo("nombre", query).whereLessThan("nombre", query + '\uf8ff').get().await()
        val list = snapshot.documents.mapNotNull { doc ->
            doc.toObject(MaterialItem::class.java)?.copy(id = doc.id)
        }
        Result.success(list)
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun searchByCode(code: String): Result<MaterialItem?> = try {
        // Buscar por código o por ID
        val snapshot = collection.whereEqualTo("codigo", code).limit(1).get().await()
        if (snapshot.documents.isNotEmpty()) {
            val material = snapshot.documents.first().toObject(MaterialItem::class.java)?.copy(id = snapshot.documents.first().id)
            Result.success(material)
        } else {
            // Intentar buscar por ID si no se encontró por código
            val byId = collection.document(code).get().await()
            if (byId.exists()) {
                Result.success(byId.toObject(MaterialItem::class.java)?.copy(id = byId.id))
            } else {
                Result.success(null)
            }
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun getMaterialById(id: String): Result<MaterialItem?> = try {
        val doc = collection.document(id).get().await()
        if (doc.exists()) {
            Result.success(doc.toObject(MaterialItem::class.java)?.copy(id = doc.id))
        } else {
            Result.success(null)
        }
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun createMaterial(material: MaterialItem): Result<MaterialItem> = try {
        val materialData = material.copy(
            fechaCreacion = System.currentTimeMillis(),
            fechaActualizacion = System.currentTimeMillis()
        )
        val docRef = if (material.id.isNotEmpty()) {
            collection.document(material.id)
        } else {
            collection.document()
        }
        docRef.set(materialData.copy(id = docRef.id)).await()
        Result.success(materialData.copy(id = docRef.id))
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun updateMaterial(material: MaterialItem): Result<MaterialItem> = try {
        val updatedMaterial = material.copy(fechaActualizacion = System.currentTimeMillis())
        collection.document(material.id).set(updatedMaterial).await()
        Result.success(updatedMaterial)
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun deleteMaterial(materialId: String): Result<Unit> = try {
        collection.document(materialId).delete().await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun getMaterialsByCategory(category: String): Result<List<MaterialItem>> = try {
        val snapshot = collection.whereEqualTo("categoria", category).get().await()
        val list = snapshot.documents.mapNotNull { doc ->
            doc.toObject(MaterialItem::class.java)?.copy(id = doc.id)
        }
        Result.success(list)
    } catch (t: Throwable) {
        Result.failure(t)
    }
    
    override suspend fun getLowStockMaterials(): Result<List<MaterialItem>> = try {
        val snapshot = collection.get().await()
        val lowStock = snapshot.documents.mapNotNull { doc ->
            val material = doc.toObject(MaterialItem::class.java)?.copy(id = doc.id)
            // Retornar solo si cantidad <= stockMinimo
            if (material != null && material.cantidad <= material.stockMinimo) {
                material
            } else null
        }
        Result.success(lowStock)
    } catch (t: Throwable) {
        Result.failure(t)
    }

    override suspend fun registerMovement(materialId: String, delta: Int): Result<Unit> = try {
        val ref = collection.document(materialId)
        db.runTransaction { txn ->
            val snap = txn.get(ref)
            val current = (snap.getLong("cantidad") ?: 0L).toInt()
            val newQuantity = current + delta
            
            // Validación de stock negativo
            if (newQuantity < 0) {
                throw IllegalStateException(
                    "Stock insuficiente. Disponible: $current unidades, Solicitado: ${-delta} unidades"
                )
            }
            
            txn.update(ref, "cantidad", newQuantity)
            txn.update(ref, "fechaActualizacion", Timestamp.now())
        }.await()
        
        moves.add(
            mapOf(
                "materialId" to materialId,
                "delta" to delta,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }

    override suspend fun totalStock(): Result<Int> = try {
        val snapshot = collection.get().await()
        val sum = snapshot.documents.sumOf { (it.getLong("cantidad") ?: 0L).toInt() }
        Result.success(sum)
    } catch (t: Throwable) { Result.failure(t) }

    override suspend fun recentMovements(limit: Int): Result<List<Movement>> = try {
        // Verificar autenticación
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        println("🔐 [Movements] recentMovements - Usuario autenticado: ${currentUser != null}")
        
        // Intentar ordenar por timestamp primero, si falla intentar por fecha
        val snap = try {
            moves.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
        } catch (e: Exception) {
            println("⚠️ [Movements] No se puede ordenar por 'timestamp', intentando por 'fecha': ${e.message}")
            try {
                moves.orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
            } catch (e2: Exception) {
                println("⚠️ [Movements] No se puede ordenar, obteniendo sin orden: ${e2.message}")
                moves.limit(limit.toLong()).get().await()
            }
        }
        
        val list = snap.documents.mapNotNull { d ->
            try {
                // Intentar leer estructura nueva (materialId, delta, timestamp)
                val materialId = d.getString("materialId")
                val delta = d.getLong("delta")
                val timestamp = d.getLong("timestamp")
                
                if (materialId != null && delta != null && timestamp != null) {
                    Movement(
                        id = d.id,
                        materialId = materialId,
                        delta = delta.toInt(),
                        timestamp = timestamp,
                        userId = d.getString("userId")
                    )
                } else {
                    // Intentar leer estructura antigua (material, cantidad, fecha, tipo)
                    val material = d.getString("material") ?: ""
                    val cantidad = d.getLong("cantidad") ?: 0L
                    val fechaTimestamp = d.getTimestamp("fecha")
                    val tipo = d.getString("tipo") ?: "salida"
                    
                    if (material.isNotEmpty() && fechaTimestamp != null) {
                        // Convertir tipo a delta: "entrada" = positivo, "salida" = negativo
                        val deltaValue = if (tipo.lowercase() == "entrada") cantidad.toInt() else -cantidad.toInt()
                        // Convertir Timestamp a Long (milisegundos)
                        val timestampValue = fechaTimestamp.toDate().time
                        
                        Movement(
                            id = d.id,
                            materialId = material, // Usar el nombre del material como ID temporal
                            delta = deltaValue,
                            timestamp = timestampValue,
                            userId = d.getString("userId")
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                println("⚠️ [Movements] Error procesando documento ${d.id}: ${e.message}")
                null
            }
        }
        println("✅ [Movements] recentMovements - Encontrados ${list.size} movimientos")
        Result.success(list)
    } catch (t: Throwable) {
        println("❌ [Movements] recentMovements - Error: ${t.message}")
        t.printStackTrace()
        Result.failure(t)
    }

    override suspend fun getMovementsInLast24Hours(): Result<List<Movement>> {
        return try {
            // Verificar autenticación primero
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            println("🔐 [Movements] Usuario autenticado: ${currentUser != null}")
            if (currentUser != null) {
                println("   📧 Email: ${currentUser.email}")
                println("   🆔 UID: ${currentUser.uid}")
            } else {
                println("⚠️ [Movements] ADVERTENCIA: Usuario NO autenticado - Las reglas de Firestore pueden bloquear el acceso")
            }
            
            val dayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            println("🔍 [Movements] Buscando movimientos desde: ${java.util.Date(dayStart)}")
            println("🔍 [Movements] Timestamp actual: ${System.currentTimeMillis()}")
            println("🔍 [Movements] Diferencia: ${System.currentTimeMillis() - dayStart} ms (${(System.currentTimeMillis() - dayStart) / (1000 * 60 * 60)} horas)")
            
            // Intentar con filtro y ordenamiento (requiere índice compuesto)
            val snap = try {
                moves
                    .whereGreaterThanOrEqualTo("timestamp", dayStart)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // Si falla por falta de índice o campo, obtener todos los movimientos recientes y filtrar en memoria
                println("⚠️ [Movements] Índice no encontrado o campo diferente, usando método alternativo: ${e.message}")
                val allRecent = try {
                    moves
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(500)
                        .get()
                        .await()
                } catch (e2: Exception) {
                    println("⚠️ [Movements] Intentando ordenar por 'fecha': ${e2.message}")
                    try {
                        moves
                            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(500)
                            .get()
                            .await()
                    } catch (e3: Exception) {
                        println("⚠️ [Movements] Obteniendo sin orden: ${e3.message}")
                        moves.limit(500).get().await()
                    }
                }
                
                // Filtrar en memoria los que están en las últimas 24 horas
                val filtered = allRecent.documents.filter { doc ->
                    try {
                        val ts = doc.getLong("timestamp")
                        if (ts != null) {
                            ts >= dayStart
                        } else {
                            val fechaTimestamp = doc.getTimestamp("fecha")
                            if (fechaTimestamp != null) {
                                fechaTimestamp.toDate().time >= dayStart
                            } else {
                                false
                            }
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Convertir documentos filtrados a Movement (soporta ambas estructuras)
                val list = filtered.mapNotNull { d ->
                    try {
                        // Intentar leer estructura nueva (materialId, delta, timestamp)
                        val materialId = d.getString("materialId")
                        val delta = d.getLong("delta")
                        val timestamp = d.getLong("timestamp")
                        
                        if (materialId != null && delta != null && timestamp != null) {
                            Movement(
                                id = d.id,
                                materialId = materialId,
                                delta = delta.toInt(),
                                timestamp = timestamp,
                                userId = d.getString("userId")
                            )
                        } else {
                            // Intentar leer estructura antigua (material, cantidad, fecha, tipo)
                            val material = d.getString("material") ?: ""
                            val cantidad = d.getLong("cantidad") ?: 0L
                            val fechaTimestamp = d.getTimestamp("fecha")
                            val tipo = d.getString("tipo") ?: "salida"
                            
                            if (material.isNotEmpty() && fechaTimestamp != null) {
                                val deltaValue = if (tipo.lowercase() == "entrada") cantidad.toInt() else -cantidad.toInt()
                                val timestampValue = fechaTimestamp.toDate().time
                                
                                Movement(
                                    id = d.id,
                                    materialId = material,
                                    delta = deltaValue,
                                    timestamp = timestampValue,
                                    userId = d.getString("userId")
                                )
                            } else {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ [Movements] Error procesando documento ${d.id}: ${e.message}")
                        null
                    }
                }
                println("✅ [Movements] Encontrados ${list.size} movimientos en las últimas 24 horas (filtrado en memoria)")
                if (list.isNotEmpty()) {
                    println("   📋 Primer movimiento: materialId=${list.first().materialId}, delta=${list.first().delta}, timestamp=${java.util.Date(list.first().timestamp)}")
                }
                return Result.success(list)
            }
            
            val list = snap.documents.mapNotNull { d ->
                try {
                    // Intentar leer estructura nueva (materialId, delta, timestamp)
                    val materialId = d.getString("materialId")
                    val delta = d.getLong("delta")
                    val timestamp = d.getLong("timestamp")
                    
                    if (materialId != null && delta != null && timestamp != null) {
                        Movement(
                            id = d.id,
                            materialId = materialId,
                            delta = delta.toInt(),
                            timestamp = timestamp,
                            userId = d.getString("userId")
                        )
                    } else {
                        // Intentar leer estructura antigua (material, cantidad, fecha, tipo)
                        val material = d.getString("material") ?: ""
                        val cantidad = d.getLong("cantidad") ?: 0L
                        val fechaTimestamp = d.getTimestamp("fecha")
                        val tipo = d.getString("tipo") ?: "salida"
                        
                        if (material.isNotEmpty() && fechaTimestamp != null) {
                            val deltaValue = if (tipo.lowercase() == "entrada") cantidad.toInt() else -cantidad.toInt()
                            val timestampValue = fechaTimestamp.toDate().time
                            
                            Movement(
                                id = d.id,
                                materialId = material,
                                delta = deltaValue,
                                timestamp = timestampValue,
                                userId = d.getString("userId")
                            )
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ [Movements] Error procesando documento ${d.id}: ${e.message}")
                    null
                }
            }
            
            println("✅ [Movements] Encontrados ${list.size} movimientos en las últimas 24 horas")
            if (list.isNotEmpty()) {
                println("   📋 Primer movimiento: materialId=${list.first().materialId}, delta=${list.first().delta}, timestamp=${java.util.Date(list.first().timestamp)}")
                println("   📋 Último movimiento: materialId=${list.last().materialId}, delta=${list.last().delta}, timestamp=${java.util.Date(list.last().timestamp)}")
            } else {
                println("⚠️ [Movements] No se encontraron movimientos en las últimas 24 horas")
                println("   💡 Verifica:")
                println("      1. Que las reglas de Firestore estén aplicadas")
                println("      2. Que el usuario esté autenticado")
                println("      3. Que existan movimientos en Firebase con timestamps recientes")
            }
            Result.success(list)
        } catch (t: Throwable) {
            println("❌ [Movements] Error obteniendo movimientos de últimas 24h: ${t.message}")
            t.printStackTrace()
            Result.failure(t)
        }
    }
}


