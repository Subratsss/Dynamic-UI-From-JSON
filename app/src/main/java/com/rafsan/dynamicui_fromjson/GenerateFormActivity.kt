package com.rafsan.dynamicui_fromjson

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.InputFilter.LengthFilter
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.NavUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rafsan.dynamicui_fromjson.databinding.ActivityGenerateFormBinding
import com.rafsan.dynamicui_fromjson.model.FormComponent
import com.rafsan.dynamicui_fromjson.model.FormComponentItem
import com.rafsan.dynamicui_fromjson.model.FormViewComponent
import com.rafsan.dynamicui_fromjson.model.Value
import com.rafsan.dynamicui_fromjson.utils.Utils
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.getCurrentDate
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.getCustomColorStateList
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.getDateFromString
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.getDateStringToShow
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.method
import com.rafsan.dynamicui_fromjson.utils.Utils.Companion.setMerginToviews
import java.util.*

class GenerateFormActivity : AppCompatActivity() {

    lateinit var binding: ActivityGenerateFormBinding
    var formViewCollection: ArrayList<FormViewComponent> = arrayListOf()

    var submitRootJsonObj: JsonObject? = null
    var submitPropertyArrayJson: JsonArray? = null
    var formComponent: FormComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val json = intent.getStringExtra("value")
        submitRootJsonObj = JsonObject()
        submitPropertyArrayJson = JsonArray()
        json?.let {
            populateForm(it)
        }
    }

    private fun populateForm(json: String) {
        formComponent = Gson().fromJson(json, FormComponent::class.java)
        var viewId = 1
        binding.miniAppFormContainer.setVisibility(View.VISIBLE)

        //TODO:- GENERATE FORM LAYOUT
        formComponent?.let {
            it.forEach { component ->
                when (component.type) {
                    "header" -> binding.miniAppFormContainer.addView(
                        createHeaderView(
                            component
                        )
                    )
                    "text" -> createEditableTextWithLabel(component, viewId++)
                    "textarea" -> createEditableTextWithLabel(component, viewId++)
                    "select" -> createSpinner(component, viewId++)
                    "radio-group" -> createRadioGroup(component, viewId++)
                    "paragraph" -> createParagraph(component)
                    "date" -> createDatePicker(component)
                    "checkbox-group" -> createCheckBoxGroup(component, viewId++)
                    "number" -> createNumberEditText(component)
                }
            }
        }
    }

    private fun createHeaderView(componentItem: FormComponentItem): TextView {
        val txtHeader = TextView(this)
        when (componentItem.subtype) {
            "h1" -> txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            "h2" -> txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            "h3" -> txtHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        componentItem.label?.let {
            txtHeader.setText(Utils.fromHtml(it))
        }

        txtHeader.layoutParams = LinearLayoutCompat.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        txtHeader.setTextColor(Color.parseColor("#000000"))
        txtHeader.setPadding(0, 15, 0, 15)
        txtHeader.gravity = Gravity.CENTER
        return txtHeader
    }

    private fun createEditableTextWithLabel(component: FormComponentItem, viewId: Int) {
        isLabelNull(component)
        val editText = EditText(this)
        var rows = 1

        setMerginToviews(
            editText, 20,
            LinearLayoutCompat.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (component.type.equals("textarea")) editText.gravity = Gravity.NO_GRAVITY

        editText.setPadding(20, 30, 20, 30)
        editText.setBackgroundResource(R.drawable.edit_text_background)
        editText.id = viewId
        isValueNull(component, editText)
        isSubTypeNull(component, editText)
        isPlaceHolderNull(component, editText)
        component.maxlength?.let {
            editText.filters =
                arrayOf<InputFilter>(LengthFilter(it.toInt()))
        }

        component.rows?.let {
            rows = it.toInt()
            val finalRow = rows
            editText.setOnKeyListener { v, keyCode, event ->
                (v as EditText).lineCount > finalRow
            }
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (editText.lineCount > finalRow) {
                        editText.setText(method(editText.text.toString()))
                        editText.setSelection(editText.text.toString().length)
                    }
                }
            })
        }
        editText.setLines(rows)
        binding.miniAppFormContainer.addView(editText)
        formViewCollection.add(FormViewComponent(editText, component))
        Log.i("EditTextInputType", editText.inputType.toString() + "")
    }

    private fun createSpinner(component: FormComponentItem, viewId: Int) {
        var selectedIndex = 0
        createLabelForViews(component)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(40, 30, 40, 40)
        val spinner = Spinner(this)
        spinner.id = viewId
        spinner.setBackgroundColor(Color.WHITE)
        spinner.setBackgroundResource(R.drawable.edit_text_background)
        spinner.layoutParams = layoutParams
        //Spinner data source
        val spinnerDatasource = mutableListOf<Any?>()
        component.values?.let {
            for (j in it.indices) {
                val value = it[j]
                spinnerDatasource.add(value.label)
                value.selected?.let { selected ->
                    if (selected)
                        selectedIndex = j
                }
            }

            val spinnerAdapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
                applicationContext,
                R.layout.form_spinner_row, spinnerDatasource
            )
            spinner.adapter = spinnerAdapter
            spinner.setSelection(selectedIndex)
        }
        binding.miniAppFormContainer.addView(spinner)
        formViewCollection.add(FormViewComponent(spinner, component))
    }

    @SuppressLint("RestrictedApi")
    private fun createRadioGroup(component: FormComponentItem, viewId: Int) {
        createLabelForViews(component)
        var selectedItem = 0
        var isRadioButtonSelected = false

        val radioGroup = RadioGroup(this)
        radioGroup.id = viewId
        Log.i("RadioGroupId", radioGroup.id.toString() + " " + radioGroup.tag)
        radioGroup.orientation = LinearLayout.VERTICAL
        setMerginToviews(radioGroup)

        component.values?.let {
            for (i in it.indices) {
                val value = it[i]
                val radioButton = AppCompatRadioButton(this)
                radioButton.setText(value.label)
                radioButton.supportButtonTintList = getCustomColorStateList(this)

                value.selected?.let { selected ->
                    if (selected) {
                        selectedItem = i
                        isRadioButtonSelected = true
                    }
                }
                radioGroup.addView(radioButton)
            }
            component.toggle?.let { selected ->
                if (selected) {
                    val radioGroupContainer = RelativeLayout(this)
                    var valueModels: MutableList<Value> = mutableListOf()
                    component.values.let {
                        valueModels = it as MutableList<Value>
                    }
                    val valueModel = Value("Other", null, null)
                    valueModels.add(valueModel)
                    val radioButton = AppCompatRadioButton(this)
                    radioButton.setText(valueModel.label)
                    radioButton.supportButtonTintList = getCustomColorStateList(this)
                    radioGroup.addView(radioButton)
                    radioGroupContainer.addView(radioGroup)
                    val otherText = EditText(this)
                    otherText.setBackgroundResource(R.drawable.edit_text_background)
                    otherText.isEnabled = false
                    val otherTextParam = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    otherTextParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    otherTextParam.addRule(RelativeLayout.RIGHT_OF, radioGroup.id)
                    //otherTextParam.setMargins(10, 0, 40, 0);
                    otherText.layoutParams = otherTextParam
                    otherText.setPadding(10, 8, 10, 8)
                    otherText.maxLines = 1
                    radioGroupContainer.addView(otherText)
                    otherText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable) {
                            if (otherText.text.toString() != "") {
                                valueModel.value = otherText.text.toString()
                            }
                        }
                    })
                    radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                        otherText.isEnabled = isChecked
                    }
                    binding.miniAppFormContainer.addView(radioGroupContainer)
                    formViewCollection.add(FormViewComponent(radioGroup, component))
                } else {
                    binding.miniAppFormContainer.addView(radioGroup)
                    formViewCollection.add(FormViewComponent(radioGroup, component))
                }
            }
            if (isRadioButtonSelected) (radioGroup.getChildAt(selectedItem) as RadioButton).isChecked =
                true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createParagraph(component: FormComponentItem) {
        val textView = TextView(this)

        val paragraphText: String =
            component.label?.substring(0, component.label.length - 2) ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (component.subtype.equals("blockquote")) {
                textView.text =
                    "\"" + Html.fromHtml(paragraphText, Html.FROM_HTML_MODE_LEGACY) + "\""
                textView.setTypeface(null, Typeface.ITALIC)
            } else textView.text = Html.fromHtml(paragraphText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            if (component.subtype.equals("blockquote")) {
                textView.text = "\"" + Html.fromHtml(paragraphText + "\"")
                textView.setTypeface(null, Typeface.ITALIC)
            } else textView.text = Html.fromHtml(paragraphText)
        }

        textView.setTextColor(Color.parseColor("#000000"))
        setMerginToviews(textView)
        textView.setPadding(0, 10, 0, 10)
        binding.miniAppFormContainer.addView(textView)
    }

    private fun createDatePicker(component: FormComponentItem) {
        val relativeLayout = RelativeLayout(this)
        relativeLayout.setPadding(5, 10, 5, 10)
        val layoutParams = LinearLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(40, 40, 40, 40)
        relativeLayout.layoutParams = layoutParams

        component.label?.let { labelString ->
            val textView = TextView(this)
            textView.setTextColor(Color.BLACK)
            textView.setTypeface(null, Typeface.BOLD)
            component.required?.let { required ->
                if (required) {
                    textView.setText(labelStringForRequiredField(labelString))
                } else {
                    textView.text = createStringForViewLabel(false, labelString)
                }
            }
            val layoutParams1 = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            layoutParams1.addRule(RelativeLayout.CENTER_VERTICAL)
            relativeLayout.addView(textView)
        }

        val txtDate = TextView(this)
        val layoutParams1 = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layoutParams1.addRule(RelativeLayout.CENTER_VERTICAL)
        txtDate.layoutParams = layoutParams1
        relativeLayout.addView(txtDate)

        txtDate.text = getDateStringToShow(getCurrentDate(), "MMMM d, yyyy")

        relativeLayout.setOnClickListener {
            val calendar: Calendar = GregorianCalendar()
            calendar.time = getDateFromString(txtDate.text.toString(), "MMMM d, yyyy")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val datePickerDialog = DatePickerDialog(
                    this@GenerateFormActivity,
                    { view, year, month, dayOfMonth ->
                        val selectedDate = Calendar.getInstance()
                        selectedDate[year, month] = dayOfMonth
                        txtDate.setText(getDateStringToShow(selectedDate.time, "MMMM d, yyyy"))
                    }, calendar[Calendar.YEAR], calendar[Calendar.MONTH],
                    calendar[Calendar.DAY_OF_MONTH]
                )
                datePickerDialog.show()
            }
        }
        binding.miniAppFormContainer.addView(relativeLayout)
        formViewCollection.add(FormViewComponent(txtDate, component))
    }

    private fun createNumberEditText(component: FormComponentItem) {

    }

    private fun createCheckBoxGroup(component: FormComponentItem, viewId: Int) {

    }

    private fun isLabelNull(viewComponentModel: FormComponentItem) {
        if (viewComponentModel.label != null) createLabelForViews(viewComponentModel)
    }

    private fun isSubTypeNull(viewComponentModel: FormComponentItem, editText: EditText) {
        if (viewComponentModel.subtype != null) {
            setInputTypeForEditText(editText, viewComponentModel)
        }
    }

    private fun isPlaceHolderNull(
        viewComponentModel: FormComponentItem,
        editText: EditText
    ) {
        if (viewComponentModel.placeholder != null) editText.setHint(viewComponentModel.placeholder)
    }

    private fun isValueNull(viewComponentModel: FormComponentItem, view: TextView) {
        viewComponentModel.value?.let {
            view.text = it
        }
    }

    private fun createLabelForViews(viewComponentModel: FormComponentItem) {
        val label = TextView(this)
        label.setTextColor(Color.BLACK)
        label.setTypeface(null, Typeface.BOLD)
        setMerginToviews(
            label,
            40,
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT
        )
        viewComponentModel.label?.let { labelText ->
            viewComponentModel.required?.let {
                if (it) {
                    label.text = createStringForViewLabel(it, labelText)
                } else {
                    label.setText(createStringForViewLabel(false, labelText))
                }
            }
            binding.miniAppFormContainer.addView(label)
        }
    }

    /**
     * EditText Input type selection
     */
    private fun setInputTypeForEditText(
        editText: EditText,
        viewComponentModel: FormComponentItem
    ) {
        when (viewComponentModel.subtype) {
            "password" -> editText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            "email" -> editText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            "tel" -> editText.inputType = InputType.TYPE_CLASS_PHONE
            "dateTimeLocal" -> editText.inputType = InputType.TYPE_CLASS_DATETIME
            else -> editText.inputType = InputType.TYPE_CLASS_TEXT
        }
    }

    private fun createStringForViewLabel(
        required: Boolean,
        label: String
    ): SpannableStringBuilder? {
        val labelStr = Utils.fromHtml(label)
        return if (required) {
            labelStringForRequiredField(labelStr)
        } else {
            val username = SpannableString(labelStr)
            val description = SpannableStringBuilder()
            username.setSpan(
                RelativeSizeSpan(1.1f), 0, username.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            username.setSpan(
                ForegroundColorSpan(Color.parseColor("#000000")), 0, username.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            description.append(username)
            description
        }
    }

    private fun labelStringForRequiredField(label: String): SpannableStringBuilder? {
        val username = SpannableString(label)
        val description = SpannableStringBuilder()
        username.setSpan(
            RelativeSizeSpan(1.1f), 0, username.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        username.setSpan(
            ForegroundColorSpan(Color.parseColor("#000000")), 0, username.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        description.append(username)
        val commentSpannable = SpannableString(" *")
        commentSpannable.setSpan(
            ForegroundColorSpan(Color.RED), 0,
            commentSpannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        commentSpannable.setSpan(
            RelativeSizeSpan(1.0f), 0,
            commentSpannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        description.append(commentSpannable)
        return description
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}