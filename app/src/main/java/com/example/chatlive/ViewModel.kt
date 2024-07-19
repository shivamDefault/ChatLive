package com.example.chatlive

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import com.example.chatlive.data.CHATS
import com.example.chatlive.data.ChatData
import com.example.chatlive.data.ChatUser
import com.example.chatlive.data.Event
import com.example.chatlive.data.MESSAGE
import com.example.chatlive.data.Message
import com.example.chatlive.data.STATUS
import com.example.chatlive.data.Status
import com.example.chatlive.data.USER_NODE
import com.example.chatlive.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    val auth: FirebaseAuth, var db: FirebaseFirestore, val storage: FirebaseStorage
) : ViewModel() {


    var inProcess = mutableStateOf(false)
    var inProcessChats = mutableStateOf(false)
    val eventMutableState = mutableStateOf<Event<String>?>(null)
    var signIn = mutableStateOf(false)
    var userData = mutableStateOf<UserData?>(null)
    val chats = mutableStateOf<List<ChatData>>(listOf())
    val chatMessages = mutableStateOf<List<Message>>(listOf())
    val inProgressChatMessage = mutableStateOf(false)
    var currentChatMessageListener: ListenerRegistration? = null
    val status = mutableStateOf<List<Status>>(emptyList())
    val inProgressStatus = mutableStateOf(false)


    init {
        val currentUser = auth.currentUser
        signIn.value = currentUser != null
        currentUser?.uid?.let {
            getUserData(it)
        }
    }

    fun populateMessages(chatId: String) {
        inProgressChatMessage.value = true
        currentChatMessageListener = db.collection(CHATS).document(chatId).collection(MESSAGE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)

                }
                if (value != null) {
                    chatMessages.value = value.documents.mapNotNull {
                        it.toObject<Message>()
                    }.sortedBy { it.timestamp }
                    inProgressChatMessage.value = false
                }
            }
    }

    fun depopulateMessage() {
        chatMessages.value = listOf()
        currentChatMessageListener = null
    }

    fun populateChats() {
        inProcessChats.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)

            }
            if (value != null) {
                chats.value = value.documents.mapNotNull {
                    it.toObject<ChatData>()
                }
                inProcessChats.value = false
            }
        }
    }


    fun signUp(name: String, number: String, email: String, password: String) {
        inProcess.value = true
        if (name.isEmpty() or number.isEmpty() or email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please Fill All fields")
            return
        }
        inProcess.value = true
        db.collection(USER_NODE).whereEqualTo("number", number).get().addOnSuccessListener {
            if (it.isEmpty) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        signIn.value = true
                        createOrUpdateProfile(name, number)
                        Log.d("TAG", "signup: User Logged IN")
                    } else {
                        handleException(it.exception, customMessage = "SignUp failed")
                    }
                }
            } else {
                handleException(customMessage = "Number Already Exits")
                inProcess.value = false
            }
        }


    }

    fun loginIn(email: String, password: String) {
        if (email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please Fill the all Fields")
            return
        } else {
            inProcess.value = true
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    signIn.value = true
                    inProcess.value = false
                    auth.currentUser?.uid?.let {
                        getUserData(it)
                    }
                } else {
                    handleException(exception = it.exception, customMessage = " Login Failed")
                }
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        uploadImage(uri) {
            createOrUpdateProfile(imageurl = it.toString())

        }
    }

    fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProcess.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("images/$uuid")
        val uploadTask = imageRef.putFile(uri).addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl
            result?.addOnSuccessListener(onSuccess)
            inProcess.value = false
        }.addOnFailureListener {
            handleException(it)
        }
    }

    fun createOrUpdateProfile(
        name: String? = null, number: String? = null, imageurl: String? = null
    ) {
        val uid = auth.currentUser?.uid
        val currentUserData = userData.value
        val updatedUserData = UserData(
            userId = uid,
            name = name ?: currentUserData?.name,
            number = number ?: currentUserData?.number,
            imageUrl = imageurl ?: currentUserData?.imageUrl
        )
        uid?.let { uid ->
            inProcess.value = true
            db.collection(USER_NODE).document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Document exists, update it
                    db.collection(USER_NODE).document(uid).set(updatedUserData)
                        .addOnSuccessListener {
                            // Update the local state after successful update
                            userData.value = updatedUserData
                            inProcess.value = false
                        }.addOnFailureListener { exception ->
                            handleException(exception, "Failed to update user data")
                        }
                } else {
                    // Document does not exist, create a new one
                    db.collection(USER_NODE).document(uid).set(updatedUserData)
                        .addOnSuccessListener {
                            userData.value = updatedUserData
                            inProcess.value = false
                        }.addOnFailureListener { exception ->
                            handleException(exception, "Failed to create user data")
                        }
                }
            }.addOnFailureListener { exception ->
                handleException(exception, "Cannot retrieve user")
            }
        }
    }


    private fun getUserData(uid: String) {
        inProcess.value = true
        db.collection(USER_NODE).document(uid).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error, "Can not Retrieve User")
            }
            if (value != null) {
                var user = value.toObject<UserData>()
                userData.value = user
                inProcess.value = false
                populateChats()
                populateStatuses()
            }


        }
    }

    fun handleException(exception: Exception? = null, customMessage: String = "") {
        Log.e("ChatApplication", "Live chat exception", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isNullOrEmpty()) errorMsg else customMessage
        eventMutableState.value = Event(message)
        inProcess.value = false

    }

    fun logOut() {
        auth.signOut()
        signIn.value = false
        userData.value = null
        eventMutableState.value = Event("LOGGED OUT")
        depopulateMessage()
        currentChatMessageListener = null
    }


    fun onAddChat(number: String) {
        if (number.isEmpty() or !number.isDigitsOnly()) {
            handleException(customMessage = "The Number must contain all Digits")
            return
        }
        db.collection(CHATS).where(
            Filter.or(
                Filter.and(
                    Filter.equalTo("user1.number", number),
                    Filter.equalTo("user2.number", userData.value?.number)
                ), Filter.and(
                    Filter.equalTo("user2.number", number),
                    Filter.equalTo("user1.number", userData.value?.number)
                )
            )
        ).get().addOnSuccessListener { chatQuerySnapshot ->
            if (chatQuerySnapshot.isEmpty) {
                db.collection(USER_NODE).whereEqualTo("number", number).get()
                    .addOnSuccessListener { userQuerySnapshot ->
                        if (userQuerySnapshot.isEmpty) {
                            handleException(customMessage = "number not found")
                        } else {
                            val chatPartner = userQuerySnapshot.toObjects<UserData>()[0]
                            val id = db.collection(CHATS).document().id
                            val chat = ChatData(
                                chatId = id, ChatUser(
                                    userData.value?.userId,
                                    userData.value?.name,
                                    userData.value?.imageUrl,
                                    userData.value?.number
                                ), ChatUser(
                                    chatPartner.userId,
                                    chatPartner.name,
                                    chatPartner.imageUrl,
                                    chatPartner.number
                                )
                            )
                            db.collection(CHATS).document(id).set(chat).addOnSuccessListener {
                                // Update UI by populating chats again
                                populateChats()
                            }.addOnFailureListener {
                                handleException(it)
                            }
                        }
                    }.addOnFailureListener {
                        handleException(it)
                    }
            } else {
                handleException(customMessage = "chat already exist")
            }
        }.addOnFailureListener {
            handleException(it)
        }
    }


    fun onSendReply(chatID: String, message: String) {
        val time = Calendar.getInstance().time.toString()
        val msg = Message(userData.value?.userId, message, time)
        db.collection(CHATS).document(chatID).collection(MESSAGE).document().set(msg)
    }

    fun uploadStatus(uri: Uri) {
        uploadImage(uri) { imageUrl ->
            if (imageUrl != null) {
                createStatus(imageUrl)
            } else {
                handleException(customMessage = "Failed to upload image for status")
            }
        }
    }


    private fun createStatus(imageUrl: Uri) {
        val currentUserData = userData.value
        if (currentUserData != null) {
            val newStatus = Status(
                user = ChatUser(
                    currentUserData.userId,
                    currentUserData.name,
                    currentUserData.imageUrl,
                    currentUserData.number
                ), imageUrl = imageUrl.toString(), timestamp = System.currentTimeMillis()
            )
            db.collection(STATUS).add(newStatus).addOnFailureListener {
                handleException(it)
            }
        } else {
            handleException(customMessage = "User data is null")
        }
    }


    fun populateStatuses() {
        val timeDelta = 24L * 60 * 60 * 1000
        val cutOff = System.currentTimeMillis() - timeDelta
        inProgressStatus.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { chatSnapshot, chatError ->
            if (chatError != null) {
                handleException(chatError)
                return@addSnapshotListener
            }
            if (chatSnapshot != null) {
                val currentConnections = arrayListOf(userData.value?.userId)
                val chats = chatSnapshot.toObjects<ChatData>()

                chats.forEach { chat ->
                    if (chat.user1.userId == userData.value?.userId) {
                        currentConnections.add(chat.user2.userId)
                    } else {
                        currentConnections.add(chat.user1.userId)
                    }
                }

                db.collection(STATUS).whereGreaterThan("timestamp", cutOff)
                    .whereIn("user.userId", currentConnections)
                    .addSnapshotListener { statusSnapshot, statusError ->
                        if (statusError != null) {
                            handleException(statusError)
                            return@addSnapshotListener
                        }
                        if (statusSnapshot != null) {
                            val statuses = statusSnapshot.toObjects<Status>()
                            status.value = statuses
                            inProgressStatus.value = false
                        }
                    }
            }
        }
    }

    fun deleteChat(chatId: String) {
        db.collection(CHATS).document(chatId).delete().addOnSuccessListener {
                // Chat deleted successfully, update UI by repopulating chats
                populateChats()
            }.addOnFailureListener { exception ->
                handleException(exception, "Failed to delete chat")
            }
    }


}
