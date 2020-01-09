package com.example.vincentagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vincentagram.R
import com.example.vincentagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){

    var fireStore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        fireStore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailViewFragment_recyclerView.adapter = DetailViewRecyclerViewAdapter()
        view.detailViewFragment_recyclerView.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {

            fireStore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null)
                    return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView

            // User Id
            viewHolder.detailViewItem_profile_textView.text = contentDTOs[position].userId

            // Image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailViewItem_imageView_content)

            // Explain of content
            viewHolder.detailViewItem_explain_textView.text = contentDTOs[position].explain

            //likes count
            viewHolder.detailViewItem_favoriteCounter_textView.text = "Likes " + contentDTOs[position].favoriteCount

            // Profile Image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailViewItem_profile_image)

            // Like Event
            viewHolder.detailViewItem_favorite_imageView.setOnClickListener {
                favoriteEvent(position)
            }

            // Like Image view process
            if(contentDTOs!![position].favorites.containsKey(uid)){
                // 내가 좋아요 버튼을 클릭했을 경우
                viewHolder.detailViewItem_favorite_imageView.setImageResource(R.drawable.ic_favorite)
            }else{
                // 내가 좋아요 버튼을 클릭 안했을 경우
                viewHolder.detailViewItem_favorite_imageView.setImageResource(R.drawable.ic_favorite_border)
            }

            // Profile Image를 클릭했을 경우
            viewHolder.detailViewItem_profile_image.setOnClickListener {
                var userFragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("destinationUserId", contentDTOs[position].userId)
                userFragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, userFragment)?.commit()
            }

        }

        private fun favoriteEvent(position : Int){
            var tsDoc = fireStore?.collection("images")?.document(contentUidList[position])
            fireStore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    // 좋아요 버튼이 클릭되어 있을 떄
                    contentDTO.favoriteCount -= 1
                    contentDTO.favorites.remove(uid)
                }else{
                    // 좋아요 버튼이 클릭안되어 있을 때
                    contentDTO.favoriteCount += 1
                    contentDTO.favorites.put(uid!!, true)
                }

                transaction.set(tsDoc, contentDTO)
            }
        }

    }

}