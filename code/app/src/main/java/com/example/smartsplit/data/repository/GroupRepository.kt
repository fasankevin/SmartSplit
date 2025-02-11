import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.smartsplit.data.Group

class GroupRepository {

    private val db = FirebaseFirestore.getInstance()

    // This function ftches all groups from Firestore
    suspend fun getAllGroups(): List<Group> {
        return try {
            val snapshot = db.collection("groups").get().await()
            snapshot.documents.mapNotNull { document ->
                val groupId = document.id
                val groupName = document.getString("groupName") ?: return@mapNotNull null
                val userIds = document.get("userIds") as? List<String> ?: emptyList()

                Group(groupId, groupName, userIds)
            }
        } catch (e: Exception) {
            // Handle errors appropriately
            emptyList()
        }
    }
}
