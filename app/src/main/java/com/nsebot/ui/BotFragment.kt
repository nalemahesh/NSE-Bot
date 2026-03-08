// ui/BotFragment.kt
package com.nsebot.ui

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nsebot.databinding.FragmentBotBinding
import com.nsebot.viewmodel.StockViewModel

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class BotFragment : Fragment() {

    private var _binding: FragmentBotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StockViewModel by activityViewModels()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChat()
        observeViewModel()

        // Welcome message
        addBotMessage("👋 Hi! I'm your NSE F&O Bot.\nType 'help' to see what I can do!")
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.recyclerChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }

        // Send on button click
        binding.btnSend.setOnClickListener { sendMessage() }

        // Send on keyboard "done"
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        // Quick action chips
        binding.chipFnoList.setOnClickListener { sendQuery("show fno list") }
        binding.chipGainers.setOnClickListener { sendQuery("top gainers") }
        binding.chipLosers.setOnClickListener { sendQuery("top losers") }
        binding.chipHelp.setOnClickListener { sendQuery("help") }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.text?.clear()
        sendQuery(text)
    }

    private fun sendQuery(query: String) {
        addUserMessage(query)
        viewModel.processQuery(query)
    }

    private fun observeViewModel() {
        viewModel.botResponse.observe(viewLifecycleOwner) { response ->
            addBotMessage(response)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { addBotMessage("❌ Error: $it") }
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.recyclerChat.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, isUser = false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.recyclerChat.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
