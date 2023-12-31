package com.example.pms.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pms.R
import com.example.pms.adapters.CardMemberListItemsAdapter
import com.example.pms.dialogs.LabelColorListDialog
import com.example.pms.dialogs.MembersListDialog
import com.example.pms.firebase.FirestoreClass
import com.example.pms.models.Board
import com.example.pms.models.Card
import com.example.pms.models.SelectedMembers
import com.example.pms.models.Task
import com.example.pms.models.User
import com.example.pms.utils.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {

    private lateinit var toolbar : Toolbar
    private lateinit var boardDetails : Board
    private lateinit var et_name_card_details : AppCompatEditText
    private lateinit var tv_select_label_color : TextView
    private lateinit var tv_select_members : TextView
    private lateinit var tv_select_due_date : TextView
    private lateinit var btn_update_card_details : Button
    private lateinit var rv_selected_members_list : RecyclerView
    private lateinit var membersDetailsList : ArrayList<User>
    private var taskListPosition  = -1
    private var cardPosition  = -1
    private var selectedColor  = ""
    private var selectedDueDateMilliSeconds : Long  = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        initializeViews()
        getIntentData()
        setUpActionbar()

        et_name_card_details.setText(boardDetails.taskList[taskListPosition].cards[cardPosition].name)
        et_name_card_details.setSelection(et_name_card_details.text.toString().length)

        selectedColor = boardDetails.taskList[taskListPosition].cards[cardPosition].labelColor

        if (selectedColor.isNotEmpty()){
            setColor()
        }

        tv_select_label_color.setOnClickListener {
            labelColorListDialog()
        }

        tv_select_members.setOnClickListener {
            membersListDialog()
        }

        selectedDueDateMilliSeconds = boardDetails.taskList[taskListPosition].cards[cardPosition].dueDate

        if (selectedDueDateMilliSeconds > 0){
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(selectedDueDateMilliSeconds))

            tv_select_due_date.setText(selectedDate)
        }

        tv_select_due_date.setOnClickListener {
            showDataPicker()
        }

        btn_update_card_details.setOnClickListener {
            if (et_name_card_details.text.toString().isNotEmpty()){
                updateCardDetails()
            }
            else{
                Toast.makeText(this, "Enter card name", Toast.LENGTH_SHORT).show()
            }
        }

        setUpSelectedMembersList()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar_card_details_activity)
        et_name_card_details = findViewById(R.id.et_name_card_details)
        tv_select_label_color = findViewById(R.id.tv_select_label_color)
        tv_select_due_date = findViewById(R.id.tv_select_due_date)
        tv_select_members = findViewById(R.id.tv_select_members)
        btn_update_card_details = findViewById(R.id.btn_update_card_details)
        rv_selected_members_list = findViewById(R.id.rv_selected_members_list)
    }

    private fun setUpActionbar() {
        setSupportActionBar(toolbar)

        val actionbar = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        actionbar?.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        actionbar?.title = boardDetails.taskList[taskListPosition].cards[cardPosition].name

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun getIntentData(){
        if (intent.hasExtra(Constants.BOARD_DETAIL)){
            boardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }

        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            taskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }

        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            cardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }

        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            membersDetailsList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)

        finish()
    }


    private fun updateCardDetails(){
        val card = Card(et_name_card_details.text.toString(),
            boardDetails.taskList[taskListPosition].cards[cardPosition].createdBy,
            boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo, selectedColor,
            selectedDueDateMilliSeconds)

        val taskList : ArrayList<Task> = boardDetails.taskList
        taskList.removeAt(taskList.size-1)

        boardDetails.taskList[taskListPosition].cards[cardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, boardDetails)
    }

    private fun deleteCard(){
        val cardList : ArrayList<Card> = boardDetails.taskList[taskListPosition].cards

        cardList.removeAt(cardPosition)

        val taskList = boardDetails.taskList
        taskList.removeAt(taskList.size-1)

        taskList[taskListPosition].cards = cardList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, boardDetails)
    }

    private fun alertDialogForDeleteList(cardName: String) {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle("Alert")
        //set message for alert dialog
        builder.setMessage("Are you sure you want to delete $cardName.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        //performing positive action
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            dialogInterface.dismiss() // Dialog will be dismissed

            deleteCard()
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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card ->{
                alertDialogForDeleteList(boardDetails.taskList[taskListPosition].cards[cardPosition].name)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun colorsList() : ArrayList<String>{
        val colorsList : ArrayList<String> = ArrayList()
        colorsList.add("#9400D3")
        colorsList.add("#ADFF2F")
        colorsList.add("#000000")
        colorsList.add("#009900")
        colorsList.add("#FF00FF")
        colorsList.add("#F4A460")
        colorsList.add("#DEB887")
        colorsList.add("#B0C4DE")

        return colorsList
    }

    private fun setColor(){
        tv_select_label_color.text = ""
        tv_select_label_color.setBackgroundColor(Color.parseColor(selectedColor))
    }


    private fun labelColorListDialog(){
        val colorsList : ArrayList<String> = colorsList()
        val listDialog = object : LabelColorListDialog(this, colorsList, resources.getString(R.string.str_select_label_color), selectedColor){
            override fun onItemSelected(color: String) {
                selectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun membersListDialog() {

        // Here we get the updated assigned members list
        val cardAssignedMembersList =
            boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo

        if (cardAssignedMembersList.size > 0) {
            // Here we got the details of assigned members list from the global members list which is passed from the Task List screen.
            for (i in membersDetailsList.indices) {
                for (j in cardAssignedMembersList) {
                    if (membersDetailsList[i].id == j) {
                        membersDetailsList[i].selected = true
                    }
                }
            }
        } else {
            for (i in membersDetailsList.indices) {
                membersDetailsList[i].selected = false
            }
        }

        val listDialog = object : MembersListDialog(
            this@CardDetailsActivity,
            membersDetailsList,
            resources.getString(R.string.select_members)
        ) {
            override fun onItemSelected(user: User, action: String) {

                // TODO (Step 5: Here based on the action in the members list dialog update the list.)
                // START
                if (action == Constants.SELECT) {
                    if (!boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.contains(
                            user.id
                        )
                    ) {
                        boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.add(
                            user.id
                        )
                    }
                } else {
                    boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo.remove(
                        user.id
                    )

                    for (i in membersDetailsList.indices) {
                        if (membersDetailsList[i].id == user.id) {
                            membersDetailsList[i].selected = false
                        }
                    }
                }

                setUpSelectedMembersList()
                // END
            }
        }
        listDialog.show()
    }

    private fun setUpSelectedMembersList() {

        // Assigned members of the Card.
        val cardAssignedMembersList =
            boardDetails.taskList[taskListPosition].cards[cardPosition].assignedTo

        // A instance of selected members list.
        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

        // Here we got the detail list of members and add it to the selected members list as required.
        for (i in membersDetailsList.indices) {
            for (j in cardAssignedMembersList) {
                if (membersDetailsList[i].id == j) {
                    val selectedMember = SelectedMembers(
                        membersDetailsList[i].id,
                        membersDetailsList[i].image
                    )

                    selectedMembersList.add(selectedMember)
                }
            }
        }

        if (selectedMembersList.size > 0) {

            // This is for the last item to show.
            selectedMembersList.add(SelectedMembers("", ""))

            tv_select_members.visibility = View.GONE
            rv_selected_members_list.visibility = View.VISIBLE

            rv_selected_members_list.layoutManager = GridLayoutManager(this@CardDetailsActivity, 5)
            val adapter = CardMemberListItemsAdapter(this@CardDetailsActivity, selectedMembersList, true)
            rv_selected_members_list.adapter = adapter
            adapter.setOnClickListener(object :
                CardMemberListItemsAdapter.OnClickListener {
                override fun onClick() {
                    membersListDialog()
                }
            })
        } else {
            tv_select_members.visibility = View.VISIBLE
            rv_selected_members_list.visibility = View.GONE
        }
    }

    private fun showDataPicker() {
        /**
         * This Gets a calendar using the default time zone and locale.
         * The calender returned is based on the current time
         * in the default time zone with the default.
         */
        val c = Calendar.getInstance()
        val year =
            c.get(Calendar.YEAR) // Returns the value of the given calendar field. This indicates YEAR
        val month = c.get(Calendar.MONTH) // This indicates the Month
        val day = c.get(Calendar.DAY_OF_MONTH) // This indicates the Day

        /**
         * Creates a new date picker dialog for the specified date using the parent
         * context's default date picker dialog theme.
         */
        val dpd = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                /*
                  The listener used to indicate the user has finished selecting a date.
                 Here the selected date is set into format i.e : day/Month/Year
                  And the month is counted in java is 0 to 11 so we need to add +1 so it can be as selected.

                 Here the selected date is set into format i.e : day/Month/Year
                  And the month is counted in java is 0 to 11 so we need to add +1 so it can be as selected.*/

                // Here we have appended 0 if the selected day is smaller than 10 to make it double digit value.
                val sDayOfMonth = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                // Here we have appended 0 if the selected month is smaller than 10 to make it double digit value.
                val sMonthOfYear =
                    if ((monthOfYear + 1) < 10) "0${monthOfYear + 1}" else "${monthOfYear + 1}"

                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                // Selected date it set to the TextView to make it visible to user.
                tv_select_due_date.text = selectedDate

                /**
                 * Here we have taken an instance of Date Formatter as it will format our
                 * selected date in the format which we pass it as an parameter and Locale.
                 * Here I have passed the format as dd/MM/yyyy.
                 */
                /**
                 * Here we have taken an instance of Date Formatter as it will format our
                 * selected date in the format which we pass it as an parameter and Locale.
                 * Here I have passed the format as dd/MM/yyyy.
                 */
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

                // The formatter will parse the selected date in to Date object
                // so we can simply get date in to milliseconds.
                val theDate = sdf.parse(selectedDate)

                /** Here we have get the time in milliSeconds from Date object
                 */

                /** Here we have get the time in milliSeconds from Date object
                 */
                selectedDueDateMilliSeconds = theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show() // It is used to show the datePicker Dialog.
    }
}