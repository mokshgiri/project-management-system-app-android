package com.example.pms.adapters

import android.content.ClipData.Item
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pms.R
import com.example.pms.activities.TaskListActivity
import com.example.pms.models.Task
import java.util.Collections

open class TaskListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<Task>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var positionDraggedFrom = -1
    private var positionDraggedTo = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        // Here the layout params are converted dynamically according to the screen size as width is 70% and height is wrap_content.
        val layoutParams = LinearLayout.LayoutParams(
            (parent.width * 0.7).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        // Here the dynamic margins are applied to the view.
        layoutParams.setMargins((15.toDp()).toPx(), 0, (40.toDp()).toPx(), 0)
        view.layoutParams = layoutParams

        return MyViewHolder(view)
    }

    /**
     * Binds each item in the ArrayList to a view
     *
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {

            if (position == list.size - 1) {
                holder.tv_add_task_list.visibility = View.VISIBLE
                holder.ll_task_item.visibility = View.GONE
            } else {
                holder.tv_add_task_list.visibility = View.GONE
                holder.ll_task_item.visibility = View.VISIBLE
            }

            holder.tv_task_list_title.text = model.title

            holder.tv_add_task_list.setOnClickListener {

                holder.tv_add_task_list.visibility = View.GONE
                holder.cv_add_task_list_name.visibility = View.VISIBLE
            }

            holder.ib_close_list_name.setOnClickListener {
                holder.tv_add_task_list.visibility = View.VISIBLE
                holder.cv_add_task_list_name.visibility = View.GONE
            }

            holder.ib_done_list_name.setOnClickListener {
                val listName = holder.et_task_list_name.text.toString()

                if (listName.isNotEmpty()) {
                    // Here we check the context is an instance of the TaskListActivity.
                    if (context is TaskListActivity) {
                        context.createTaskList(listName)
                    }
                } else {
                    Toast.makeText(context, "Please Enter List Name.", Toast.LENGTH_SHORT).show()
                }
            }

            holder.ib_edit_list_name.setOnClickListener {

                holder.et_edit_task_list_name.setText(model.title) // Set the existing title
                holder.ll_title_view.visibility = View.GONE
                holder.cv_edit_task_list_name.visibility = View.VISIBLE
            }

            holder.ib_close_editable_view.setOnClickListener {
                holder.ll_title_view.visibility = View.VISIBLE
                holder.cv_edit_task_list_name.visibility = View.GONE
            }

            holder.ib_done_edit_list_name.setOnClickListener {
                val listName = holder.et_edit_task_list_name.text.toString()

                if (listName.isNotEmpty()) {
                    if (context is TaskListActivity) {
                        context.updateTaskList(position, listName, model)
                    }
                } else {
                    Toast.makeText(context, "Please Enter List Name.", Toast.LENGTH_SHORT).show()
                }
            }

            holder.ib_delete_list.setOnClickListener {

                alertDialogForDeleteList(position, model.title.toString())
            }

            holder.tv_add_card.setOnClickListener {

                holder.tv_add_card.visibility = View.GONE
                holder.cv_add_card.visibility = View.VISIBLE

            }

            holder.ib_close_card_name.setOnClickListener {
                holder.tv_add_card.visibility = View.VISIBLE
                holder.cv_add_card.visibility = View.GONE
            }

            holder.ib_done_card_name.setOnClickListener {

                val cardName = holder.et_card_name.text.toString()

                if (cardName.isNotEmpty()) {
                    if (context is TaskListActivity) {
                        context.addCardToTaskList(position, cardName)
                    }
                } else {
                    Toast.makeText(context, "Please Enter Card Detail.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            holder.rv_card_list.layoutManager = LinearLayoutManager(context)
            holder.rv_card_list.setHasFixedSize(true)

            val adapter =
                CardListItemsAdapter(context, model.cards)
            holder.rv_card_list.adapter = adapter

            adapter.setOnClickListener(
                object : CardListItemsAdapter.OnClickListener{
                    override fun onClick(cardPosition: Int) {

                        if (context is TaskListActivity){
                            context.cardDetails(position, cardPosition)
                        }
                    }
                }
            )

            val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            holder.rv_card_list.addItemDecoration(dividerItemDecoration)

            val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ){
                override fun onMove(
                    recyclerView: RecyclerView,
                    dragged: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val draggedPosition = dragged.adapterPosition
                    val targetPosition = target.adapterPosition

                    if (positionDraggedFrom == -1){
                        positionDraggedFrom = draggedPosition
                    }
                    positionDraggedTo = targetPosition
                    Collections.swap(list[position].cards, draggedPosition, targetPosition)

                    adapter.notifyItemMoved(draggedPosition, targetPosition)

                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)

                    if (positionDraggedFrom != -1 && positionDraggedTo != -1 && positionDraggedFrom != positionDraggedTo){

                        (context as TaskListActivity).updateCardsInTaskList(position, list[position].cards)
                    }
                    positionDraggedFrom = -1
                    positionDraggedTo = -1
                }

            })

            helper.attachToRecyclerView(holder.rv_card_list)
//            adapter.setOnClickListener(object :
//                CardListItemsAdapter.OnClickListener {
//                override fun onClick(cardPosition: Int) {
//                    if (context is TaskListActivity) {
//                        context.cardDetails(position, cardPosition)
//                    }
//                }
//            })
        }
    }


    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * A function to get density pixel from pixel
     */
    private fun Int.toDp(): Int =
        (this / Resources.getSystem().displayMetrics.density).toInt()

    /**
     * A function to get pixel from density pixel
     */
    private fun Int.toPx(): Int =
        (this * Resources.getSystem().displayMetrics.density).toInt()

    /**
     * Method is used to show the Alert Dialog for deleting the task list.
     */
    private fun alertDialogForDeleteList(position: Int, title: String) {
        val builder = AlertDialog.Builder(context)
        //set title for alert dialog
        builder.setTitle("Alert")
        //set message for alert dialog
        builder.setMessage("Are you sure you want to delete $title.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        //performing positive action
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            dialogInterface.dismiss() // Dialog will be dismissed

            if (context is TaskListActivity) {
                context.deleteTaskList(position)
            }
        }

        //performing negative action
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss() // Dialog will be dismissed
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false) // Will not allow user to cancel after clicking on remaining screen area.
        alertDialog.show()  // show the dialog to UI
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tv_add_task_list : TextView = itemView.findViewById(R.id.tv_add_task_list)
        val ll_task_item : LinearLayout = itemView.findViewById(R.id.ll_task_item)
        val tv_task_list_title : TextView = itemView.findViewById(R.id.tv_task_list_title)
        val cv_add_task_list_name : CardView = itemView.findViewById(R.id.cv_add_task_list_name)
        val ib_close_list_name : ImageButton = itemView.findViewById(R.id.ib_close_list_name)
        val ib_done_list_name : ImageButton = itemView.findViewById(R.id.ib_done_list_name)
        val et_task_list_name : EditText = itemView.findViewById(R.id.et_task_list_name)
        val ib_edit_list_name : ImageButton = itemView.findViewById(R.id.ib_edit_list_name)
        val et_edit_task_list_name : EditText = itemView.findViewById(R.id.et_edit_task_list_name)
        val ll_title_view : LinearLayout = itemView.findViewById(R.id.ll_title_view)
        val cv_edit_task_list_name : CardView = itemView.findViewById(R.id.cv_edit_task_list_name)
        val ib_close_editable_view : ImageButton = itemView.findViewById(R.id.ib_close_editable_view)
        val ib_done_edit_list_name : ImageButton = itemView.findViewById(R.id.ib_done_edit_list_name)
        val ib_delete_list : ImageButton = itemView.findViewById(R.id.ib_delete_list)
        val tv_add_card : TextView = itemView.findViewById(R.id.tv_add_card)
        val cv_add_card : CardView = itemView.findViewById(R.id.cv_add_card)
        val ib_done_card_name : ImageButton = itemView.findViewById(R.id.ib_done_card_name)
        val ib_close_card_name : ImageButton = itemView.findViewById(R.id.ib_close_card_name)
        val rv_card_list : RecyclerView = itemView.findViewById(R.id.rv_card_list)
        val et_card_name : EditText = itemView.findViewById(R.id.et_card_name)
//        val ib_done_card_name : TextView = itemView.findViewById(R.id.tv_add_task_list)
    }
}