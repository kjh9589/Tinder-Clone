package com.teamnoyes.tinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.teamnoyes.tinder.DBKey.Companion.DIS_LIKE
import com.teamnoyes.tinder.DBKey.Companion.LIKE
import com.teamnoyes.tinder.DBKey.Companion.LIKED_BY
import com.teamnoyes.tinder.DBKey.Companion.MATCH
import com.teamnoyes.tinder.DBKey.Companion.NAME
import com.teamnoyes.tinder.DBKey.Companion.USERS
import com.teamnoyes.tinder.DBKey.Companion.USER_ID
import com.teamnoyes.tinder.databinding.ActivityLikeBinding
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var activityLikeBinding: ActivityLikeBinding
    private lateinit var userDB: DatabaseReference
    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()
    private val manager by lazy { CardStackLayoutManager(this, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLikeBinding = ActivityLikeBinding.inflate(layoutInflater)
        setContentView(activityLikeBinding.root)

        auth = Firebase.auth

        userDB = Firebase.database.reference.child(USERS)

        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(NAME).value == null) {
                    showNameInputPopup()
                    return
                }

                // 유저 정보 갱신
                getUnSelectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        initCardStackView()
        initSignOutButton()
        initMatcheListButton()
    }

    private fun initCardStackView() {
        activityLikeBinding.cardStackView.layoutManager = manager
        activityLikeBinding.cardStackView.adapter = adapter
    }

    private fun initSignOutButton() {
        activityLikeBinding.signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initMatcheListButton() {
        activityLikeBinding.matchListButton.setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
    }

    private fun getUnSelectedUsers() {
        userDB.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // 현재 보고 있는 유저 아이디가 내가 아니고
                // 상대방의 likedBy에 like에 내가 없고
                // 삳대방의 likedBy에 dislike에 내가 없다
                // 한 번도 선택한 적 없는 유저
                if (snapshot.child(USER_ID).value != getCurrentUserID() &&
                        snapshot.child(LIKED_BY).child(LIKE).hasChild(getCurrentUserID()).not() &&
                        snapshot.child(LIKED_BY).child(DIS_LIKE).hasChild(getCurrentUserID()).not()){
                    val userId = snapshot.child(USER_ID).value.toString()
                    var name = "undecided"

                    if (snapshot.child(NAME).value != null){
                        name = snapshot.child(NAME).value.toString()
                    }

                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                var idx = -1
//
//                for ((index, item) in cardItems.withIndex()){
//                    if (item.userId == snapshot.key){
//                        idx = index
//                        break
//                    }
//                }
//
//                if (idx != -1){
//                    val id = cardItems[idx].userId
//                    cardItems.removeAt(idx)
//                    cardItems.add(idx, CardItem(id, snapshot.child("name").value.toString()))
//                }

                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child("name").value.toString()
                }

                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.write_name))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                if (editText.text.isEmpty()){
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user[USER_ID] = userId
        user[NAME] = name
        currentUserDB.updateChildren(user)

        // 유저 정보 가져오기
        getUnSelectedUsers()
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }

    private fun like() {
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(LIKE)
            .child(getCurrentUserID())
            .setValue(true)

        saveMatchIfOtherUserLikedMe(card.userId)

        Toast.makeText(this, "${card.name}님을 Like 하셨습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun dislike() {
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(DIS_LIKE)
            .child(getCurrentUserID())
            .setValue(true)

        Toast.makeText(this, "${card.name}님을 disLike 하셨습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtherUserLikedMe(otherUserId: String) {
        val otherUserDB = userDB.child(getCurrentUserID()).child(LIKED_BY).child(LIKE).child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true){
                    userDB.child(getCurrentUserID())
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(otherUserId)
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardSwiped(direction: Direction?) {
        when (direction){
            Direction.Right -> like()
            Direction.Left -> dislike()
            else -> {}
        }
    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}