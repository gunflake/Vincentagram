package com.example.vincentagram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.vincentagram.R
import com.example.vincentagram.navigation.model.AlarmDTO
import com.example.vincentagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    var contentUid : String? = null
    var destinationUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        activityComment_recyclerView.adapter = CommentActivityRecyclerViewAdapter()
        activityComment_recyclerView.layoutManager = LinearLayoutManager(this)


        activityComment_send_btn?.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.comment = activityComment_editText.text.toString()
            comment.uid = auth?.currentUser?.uid
            comment.userId = auth?.currentUser?.email
            comment.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document(contentUid!!)?.collection("comments")?.document()?.set(comment)
                ?.addOnSuccessListener {
                    Log.i("COMMENT", "Comment success enroll")
                }
                ?.addOnFailureListener{exception ->
                    Log.e("COMMENT", "Comment fail enroll", exception)
                }

            alarmEnroll(destinationUid, activityComment_editText.text.toString())
            activityComment_editText.setText("")
        }
    }

    fun alarmEnroll(destinationUid: String?, message: String?) {
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.email = auth?.currentUser?.email
        alarmDTO.message = message
        alarmDTO.datetime = System.currentTimeMillis()
        alarmDTO.type = 1 // Comment Type = 1

        firestore?.collection("alarms")?.document()?.set(alarmDTO)
            ?.addOnSuccessListener { Log.d("TAG", "DocumentSnapshot successfully written!") }
            ?.addOnFailureListener { e -> Log.w("TAG", "Error writing document", e) }
    }

    inner class CommentActivityRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            firestore?.collection("images")?.document(contentUid!!)?.collection("comments")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                comments.clear()
                if(querySnapshot == null)
                    return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    var getComment = snapshot.toObject(ContentDTO.Comment::class.java)
                    comments.add(getComment!!)
                    Log.i("comments data", getComment.comment)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view!!)

        override fun getItemCount(): Int {
            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHoler = (holder as CustomViewHolder).itemView
            viewHoler.itemComment_profile_id.text = comments[position].userId
            viewHoler.itemComment_comment.text = comments[position].comment

            firestore?.collection("profileImages")?.document(comments[position].uid!!)?.get()?.addOnCompleteListener {task ->
                var profileUrl = task.result!!["images"].toString()
                Glide.with(holder.itemView.context).load(profileUrl).apply(RequestOptions().circleCrop()).into(viewHoler.itemComment_profile_image)
            }


        }

    }
}
