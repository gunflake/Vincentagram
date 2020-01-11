package com.example.vincentagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.vincentagram.LoginActivity
import com.example.vincentagram.MainActivity
import com.example.vincentagram.R
import com.example.vincentagram.navigation.model.ContentDTO
import com.example.vincentagram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {

    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var curerntUseruid: String? = null

    companion object {
        var PICK_PROFILE_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)

        uid = arguments?.getString("destinationUid")
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        curerntUseruid = FirebaseAuth.getInstance().currentUser?.uid

        getFollowAndFollowing()

        if (uid == curerntUseruid) { //My Page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                auth?.signOut()
                startActivity(Intent(activity, LoginActivity::class.java))
            }
        } else { // Other User Page

            //Follow, Following 버튼을 클릭할 떄마다 해당 기능에 맞는 동작
            fragmentView?.account_btn_follow_signout?.setOnClickListener{
                requestFollow()
            }
            var mainActivity = (activity as MainActivity)

            mainActivity.toolbar_username.text = arguments?.getString("destinationUserId")
            mainActivity.toolbar_arrow_back.setOnClickListener {
                mainActivity.bottom_navigaton.selectedItemId = R.id.action_home
            }

            mainActivity.toolbar_arrow_back.visibility = View.VISIBLE
            mainActivity.toolbar_username.visibility = View.VISIBLE
            mainActivity.toolbar_logo_image.visibility = View.GONE


        }

        fragmentView?.account_recycleView?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recycleView?.layoutManager = GridLayoutManager(activity, 3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_ALBUM)
        }

        getProfileImage()

        return fragmentView
    }

    private fun getFollowAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null){
                return@addSnapshotListener
            }

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO.followingCount.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follow_count?.text = followDTO.followerCount.toString()

                if(curerntUseruid!=uid){
                    if (followDTO.followers.containsKey(curerntUseruid)) {
                        fragmentView?.account_btn_follow_signout?.text = "Following"
                    } else {
                        fragmentView?.account_btn_follow_signout?.text = "Follow"
                    }
                }
            }
        }
    }

    // Following 기능
    private fun requestFollow() {
        // save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(curerntUseruid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            // 내 계정에 연결된 follow가 한명도 없다면...
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                // TODO : 확인해보기 followers 인지 followings 인지
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // 내가 상대방을 이미 팔라우하고 있다면
            if(followDTO.followings.containsKey(uid)){
                followDTO.followingCount -= 1
                followDTO.followings.remove(uid)
            }else{ // 내가 상대방을 팔라우하고 있지 않으면
                followDTO.followingCount += 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // Save to following Account
        var tsDocFollow = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollow!!).toObject(FollowDTO::class.java)

            //상대방을 팔로우 하는 사람이 내가 처음이라면...
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[curerntUseruid!!] = true

                transaction.set(tsDocFollow, followDTO!!)
                return@runTransaction
            }

            //상대방을 이미 팔로우하고 있다면
            if(followDTO!!.followers.containsKey(curerntUseruid)){
                followDTO!!.followerCount -= 1
                followDTO!!.followers.remove(curerntUseruid)
            }else{
                followDTO!!.followerCount += 1
                followDTO!!.followers[curerntUseruid!!] = true
            }

            transaction.set(tsDocFollow, followDTO!!)
            return@runTransaction
        }
    }

    private fun getProfileImage() {
        FirebaseFirestore.getInstance().collection("profileImages").document(uid!!)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null)
                    return@addSnapshotListener

                if (documentSnapshot.data != null) {
                    var url = documentSnapshot.data!!["images"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop())
                        .into(fragmentView?.account_iv_profile!!)
                }
            }

    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null)
                        return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)


        }

    }

}