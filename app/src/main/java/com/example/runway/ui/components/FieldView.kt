package com.example.runway.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.runway.R
import com.example.runway.databinding.ViewFieldBinding
import com.google.android.material.textfield.TextInputEditText

/** A labelled text input with an optional helper or error line below it. */
class FieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewFieldBinding.inflate(LayoutInflater.from(context), this)

    /** The inner EditText, for callers that need input types or focus control. */
    val editText: TextInputEditText get() = binding.fieldInput

    init {
        orientation = VERTICAL

        context.withStyledAttributes(attrs, R.styleable.FieldView) {
            label = getString(R.styleable.FieldView_rwLabel)
            hint = getString(R.styleable.FieldView_rwHint)
        }
    }

    var label: CharSequence?
        get() = binding.fieldLabel.text
        set(value) {
            binding.fieldLabel.text = value
            // Gives TalkBack the label to read out with the input.
            binding.fieldInput.contentDescription = value
        }

    /** The value the user has typed. */
    var text: String
        get() = binding.fieldInput.text?.toString().orEmpty()
        set(value) {
            binding.fieldInput.setText(value)
        }

    /** Placeholder shown while the field is empty. */
    var placeholder: CharSequence?
        get() = binding.fieldInput.hint
        set(value) {
            binding.fieldInput.hint = value
        }

    /** Helper text below the field. Replaced by [error] while one is set. */
    var hint: CharSequence? = null
        set(value) {
            field = value
            if (error == null) showSupporting(value, isError = false)
        }

    /** Validation message. Setting it colours the border; setting it to null restores [hint]. */
    var error: CharSequence? = null
        set(value) {
            field = value
            if (value != null) {
                showSupporting(value, isError = true)
            } else {
                showSupporting(hint, isError = false)
            }
            // TextInputLayout ignores a custom error state, so the whole colour list is swapped.
            binding.fieldInputLayout.setBoxStrokeColorStateList(
                AppCompatResources.getColorStateList(
                    context,
                    if (value != null) R.color.rw_field_stroke_error else R.color.rw_field_stroke,
                )!!
            )
        }

    /** Makes the field multi-line. */
    fun setMultiline(lines: Int = DEFAULT_MULTILINE_ROWS) {
        binding.fieldInput.apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = lines
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            isSingleLine = false
        }
    }

    /** Called on every keystroke. Also clears any [error] once the user starts typing. */
    fun onTextChanged(listener: (String) -> Unit) {
        binding.fieldInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (error != null) error = null
                listener(s?.toString().orEmpty())
            }
        })
    }

    private fun showSupporting(message: CharSequence?, isError: Boolean) {
        binding.fieldHint.text = message
        binding.fieldHint.isVisible = !message.isNullOrBlank()
        binding.fieldHint.setTextColor(
            com.google.android.material.color.MaterialColors.getColor(
                this,
                if (isError) androidx.appcompat.R.attr.colorError
                else com.google.android.material.R.attr.colorOnSurfaceVariant,
            )
        )
    }

    private companion object {
        const val DEFAULT_MULTILINE_ROWS = 3
    }
}
