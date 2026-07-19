package com.mychat.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mychat.app.R
import com.mychat.app.adapters.FederChatAdapter
import com.mychat.app.models.User
import com.mychat.app.network.ApiClient
import com.mychat.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class ChatListFragment : Fragment() {

    private lateinit var chatList: RecyclerView
    private var adapter: FederChatAdapter? = null
    private val users = mutableListOf<User>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatList = view.findViewById(R.id.chatList)
        chatList.layoutManager = LinearLayoutManager(requireContext())
        loadChats()
    }

    private fun loadChats() {
        val prefs = requireActivity().getSharedPreferences("mychat_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val me = prefs.getString("username", "") ?: ""
        val server = Constants.SERVER_URL

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = ApiClient.get("$server/users/$me", token)
                if (r.isSuccessful) {
                    val arr = JSONArray(r.body!!.string())
                    val list = mutableListOf<User>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(User(
                            username = o.optString("username"),
                            name = o.optString("name", "").ifEmpty { o.optString("username") },
                            avatarColor = o.optString("avatar_color", "#2AABEE"),
                            online = o.optBoolean("online"),
                            isBot = o.optBoolean("is_bot"),
                            isGroup = o.optBoolean("is_group"),
                            isFeed = o.optBoolean("is_feed"),
                            unread = o.optInt("unread")
                        ))
                    }
                    activity?.runOnUiThread {
                        users.clear()
                        users.addAll(list)
                        if (adapter == null) {
                            adapter = FederChatAdapter { }
                            chatList.adapter = adapter
                        }
                        adapter?.update(users)
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
