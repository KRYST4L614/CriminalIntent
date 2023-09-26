package com.example.criminalintent

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.security.cert.CertPathValidatorResult
import java.util.UUID as UUID

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }
    private var adapter: CrimeEmptyAdapter? = CrimeEmptyAdapter()
    private lateinit var recyclerList: RecyclerView
    private var callbacks: Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        recyclerList = view.findViewById(R.id.recycler_crime_view) as RecyclerView
        recyclerList.layoutManager = LinearLayoutManager(context)
        recyclerList.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimesLiveData.observe(
            viewLifecycleOwner
        ) { crimes: List<Crime> ->
            if (!crimes.isEmpty() && adapter is CrimeEmptyAdapter) {
                val newAdapter = CrimeAdapter()
                recyclerList.adapter = newAdapter
                (recyclerList.adapter as CrimeAdapter).submitList(crimes.toMutableList())
            } else if (crimes.isEmpty() && adapter !is CrimeEmptyAdapter) {
                val newAdapter = CrimeEmptyAdapter()
                recyclerList.adapter = newAdapter
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }
    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.new_crime -> {
                val crime = Crime("Untitled")
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }

    private open inner class CrimeHolder(view: View) : ViewHolder(view), View.OnClickListener {
        protected open lateinit var crime: Crime
        protected open var titleTextView: TextView = view.findViewById(R.id.text_view_title)
        protected open var dateTextView: TextView = view.findViewById(R.id.text_view_date)

        init {
            itemView.setOnClickListener(this)
        }

        open fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = crime.title
            dateTextView.text = DateFormat.format("EEE, MMM d, yyy", crime.date)
        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)

        }
    }

//    private open inner class CrimeHolderPolice(view: View) : CrimeHolder(view) {
//        protected open val policeButton: Button = view.findViewById<Button?>(R.id.police_button).apply {
//            setOnClickListener {
//                Toast.makeText(
//                    this@CrimeListFragment.context,
//                    "Calling to police...",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }

    private class DiffCrimesCallback : ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime): Boolean {
            return oldItem == newItem
        }

    }

    private inner class CrimeAdapter : ListAdapter<Crime, CrimeHolder>(DiffCrimesCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolderSolve(view)
        }

        override fun getItemCount(): Int {
            return currentList.size
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            holder.bind(currentList[position])
        }

    }

    private inner class EmptyHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val textView = view.findViewById(R.id.empty_list_text_view) as TextView
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val crime = Crime("Untitled")
            crimeListViewModel.addCrime(crime)
            callbacks?.onCrimeSelected(crime.id)
        }

        fun bind() {
            textView.setOnClickListener(this)
        }
    }

    private inner class CrimeEmptyAdapter : RecyclerView.Adapter<EmptyHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmptyHolder {
            val view = layoutInflater.inflate(R.layout.list_empty_item_crime, parent, false)
            return EmptyHolder(view)
        }

        override fun getItemCount(): Int {
           return 1
        }

        override fun onBindViewHolder(holder: EmptyHolder, position: Int) {
            return holder.bind()
        }

    }

    private open inner class CrimeHolderSolve(view: View) : CrimeHolder(view) {
        protected open val solvedImageView: ImageView = view.findViewById(R.id.crime_solved)
        override fun bind(crime: Crime) {
            super.bind(crime)
            solvedImageView.visibility = if (crime.isSolved) {
                ImageView.VISIBLE
            } else {
                ImageView.GONE
            }
        }
    }

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }
}