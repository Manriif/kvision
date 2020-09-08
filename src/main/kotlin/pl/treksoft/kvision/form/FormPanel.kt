/*
 * Copyright (c) 2017-present Robert Jaros
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package pl.treksoft.kvision.form

import com.github.snabbdom.VNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import pl.treksoft.kvision.core.Container
import pl.treksoft.kvision.core.StringBoolPair
import pl.treksoft.kvision.core.StringPair
import pl.treksoft.kvision.form.FormPanel.Companion.create
import pl.treksoft.kvision.html.Div
import pl.treksoft.kvision.panel.FieldsetPanel
import pl.treksoft.kvision.panel.SimplePanel
import pl.treksoft.kvision.types.KFile
import pl.treksoft.kvision.utils.set
import kotlin.js.Date
import kotlin.js.Json
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Bootstrap form layout options.
 */
enum class FormType(internal val formType: String) {
    INLINE("form-inline"),
    HORIZONTAL("form-horizontal")
}

/**
 * Proportions for horizontal form layout.
 */
enum class FormHorizontalRatio(val labels: Int, val fields: Int) {
    RATIO_2(2, 10),
    RATIO_3(3, 9),
    RATIO_4(4, 8),
    RATIO_5(5, 7),
    RATIO_6(6, 6),
    RATIO_7(7, 5),
    RATIO_8(8, 4),
    RATIO_9(9, 3),
    RATIO_10(10, 2)
}

/**
 * Form methods.
 */
enum class FormMethod(internal val method: String) {
    GET("get"),
    POST("post")
}

/**
 * Form encoding types.
 */
enum class FormEnctype(internal val enctype: String) {
    URLENCODED("application/x-www-form-urlencoded"),
    MULTIPART("multipart/form-data"),
    PLAIN("text/plain")
}

/**
 * Form targets.
 */
enum class FormTarget(internal val target: String) {
    BLANK("_blank"),
    SELF("_self"),
    PARENT("_parent"),
    TOP("_top")
}

/**
 * Bootstrap form component.
 *
 * @constructor
 * @param K model class type
 * @param method HTTP method
 * @param action the URL address to send data
 * @param enctype form encoding type
 * @param type form layout
 * @param condensed  determines if the form is condensed.
 * @param horizRatio  horizontal form layout ratio
 * @param classes set of CSS class names
 * @param serializer a serializer for model type
 * @param customSerializers a map of custom serializers for model type
 */
@Suppress("TooManyFunctions")
open class FormPanel<K : Any>(
    method: FormMethod? = null, action: String? = null, enctype: FormEnctype? = null,
    private val type: FormType? = null, condensed: Boolean = false,
    horizRatio: FormHorizontalRatio = FormHorizontalRatio.RATIO_2, classes: Set<String> = setOf(),
    serializer: KSerializer<K>? = null, customSerializers: Map<KClass<*>, KSerializer<*>>? = null
) : SimplePanel(classes) {

    /**
     * HTTP method.
     */
    var method by refreshOnUpdate(method)

    /**
     * The URL address to send data.
     */
    var action by refreshOnUpdate(action)

    /**
     * The form encoding type.
     */
    var enctype by refreshOnUpdate(enctype)

    /**
     * The form name.
     */
    var name: String? by refreshOnUpdate()

    /**
     * The form target.
     */
    var target: FormTarget? by refreshOnUpdate()

    /**
     * Determines if the form is not validated.
     */
    var novalidate: Boolean? by refreshOnUpdate()

    /**
     * Determines if the form should have autocomplete.
     */
    var autocomplete: Boolean? by refreshOnUpdate()

    /**
     * Determines if the form is condensed.
     */
    var condensed by refreshOnUpdate(condensed)

    /**
     * Horizontal form layout ratio.
     */
    var horizRatio by refreshOnUpdate(horizRatio)

    /**
     * Function returning validation message.
     */
    var validatorMessage
        get() = form.validatorMessage
        set(value) {
            form.validatorMessage = value
        }

    /**
     * Validation function.
     */
    var validator
        get() = form.validator
        set(value) {
            form.validator = value
        }

    internal var validatorError: String?
        get() = validationAlert.content
        set(value) {
            validationAlert.content = value
            validationAlert.visible = value != null
            refresh()
        }

    /**
     * @suppress
     * Internal property.
     */
    @Suppress("LeakingThis")
    val form = Form(this, serializer, customSerializers)

    /**
     * @suppress
     * Internal property.
     */
    protected val validationAlert = Div(classes = setOf("alert", "alert-danger")).apply {
        role = "alert"
        visible = false
    }

    private var currentFieldset: FieldsetPanel? = null

    init {
        this.addPrivate(validationAlert)
    }

    override fun render(): VNode {
        return render("form", childrenVNodes())
    }

    override fun getSnClass(): List<StringBoolPair> {
        val cl = super.getSnClass().toMutableList()
        if (type != null) {
            cl.add(type.formType to true)
            if (type == FormType.HORIZONTAL) cl.add("container-fluid" to true)
        }
        if (condensed) cl.add("kv-form-condensed" to true)
        return cl
    }

    override fun getSnAttrs(): List<StringPair> {
        val sn = super.getSnAttrs().toMutableList()
        method?.let {
            sn.add("method" to it.method)
        }
        action?.let {
            sn.add("action" to it)
        }
        enctype?.let {
            sn.add("enctype" to it.enctype)
        }
        name?.let {
            sn.add("name" to it)
        }
        target?.let {
            sn.add("target" to it.target)
        }
        if (autocomplete == false) {
            sn.add("autocomplete" to "off")
        }
        if (novalidate == true) {
            sn.add("novalidate" to "novalidate")
        }
        return sn
    }

    protected fun <C : FormControl> addInternal(
        key: KProperty1<K, *>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        when (type) {
            FormType.INLINE -> control.styleForInlineFormPanel()
            FormType.HORIZONTAL -> control.styleForHorizontalFormPanel(horizRatio)
            else -> control.styleForVerticalFormPanel()
        }
        if (required) control.flabel.addCssClass("required-label")
        if (legend == null) {
            super.add(control)
        } else if (currentFieldset == null || currentFieldset?.legend != legend) {
            currentFieldset = FieldsetPanel(legend) {
                add(control)
            }
            super.add(currentFieldset!!)
        } else {
            currentFieldset?.add(control)
        }
        form.addInternal(key, control, required, requiredMessage, validatorMessage, validator)
        return this
    }

    /**
     * Adds a string control to the form panel.
     * @param key key identifier of the control
     * @param control the string form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : StringFormControl> add(
        key: KProperty1<K, String?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Adds a string control to the form panel bound to custom field type.
     * @param key key identifier of the control
     * @param control the string form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : StringFormControl> addCustom(
        key: KProperty1<K, Any?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Adds a boolean control to the form panel.
     * @param key key identifier of the control
     * @param control the boolean form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : BoolFormControl> add(
        key: KProperty1<K, Boolean?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Adds a number control to the form panel.
     * @param key key identifier of the control
     * @param control the number form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : NumberFormControl> add(
        key: KProperty1<K, Number?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Adds a date control to the form panel.
     * @param key key identifier of the control
     * @param control the date form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : DateFormControl> add(
        key: KProperty1<K, Date?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Adds a files control to the form panel.
     * @param key key identifier of the control
     * @param control the files form control
     * @param required determines if the control is required
     * @param requiredMessage optional required validation message
     * @param legend put this control inside a fieldset with given legend
     * @param validatorMessage optional function returning validation message
     * @param validator optional validation function
     * @return current form panel
     */
    open fun <C : KFilesFormControl> add(
        key: KProperty1<K, List<KFile>?>, control: C, required: Boolean = false, requiredMessage: String? = null,
        legend: String? = null,
        validatorMessage: ((C) -> String?)? = null,
        validator: ((C) -> Boolean?)? = null
    ): FormPanel<K> {
        return addInternal(key, control, required, requiredMessage, legend, validatorMessage, validator)
    }

    /**
     * Removes a control from the form panel.
     * @param key key identifier of the control
     * @return current form panel
     */
    open fun remove(key: KProperty1<K, *>): FormPanel<K> {
        form.getControl(key)?.let {
            super.remove(it)
        }
        form.remove(key)
        return this
    }

    override fun removeAll(): FormPanel<K> {
        super.removeAll()
        form.removeAll()
        return this
    }

    /**
     * Returns a control of given key.
     * @param key key identifier of the control
     * @return selected control
     */
    open fun getControl(key: KProperty1<K, *>): FormControl? {
        return form.getControl(key)
    }

    /**
     * Returns a value of the control of given key.
     * @param key key identifier of the control
     * @return value of the control
     */
    operator fun get(key: KProperty1<K, *>): Any? {
        return getControl(key)?.getValue()
    }

    /**
     * Sets the values of all the controls from the model.
     * @param model data model
     */
    open fun setData(model: K) {
        form.setData(model)
    }

    /**
     * Sets the values of all controls to null.
     */
    open fun clearData() {
        form.clearData()
    }

    /**
     * Returns current data model.
     * @return data model
     */
    open fun getData(): K {
        return form.getData()
    }

    /**
     * Returns current data model as JSON.
     * @return data model as JSON
     */
    open fun getDataJson(): Json {
        return form.getDataJson()
    }

    /**
     * Invokes validator function and validates the form.
     * @param markFields determines if form fields should be labeled with error messages
     * @return validation result
     */
    open fun validate(markFields: Boolean = true): Boolean {
        return form.validate(markFields)
    }

    companion object {

        inline fun <reified K : Any> create(
            method: FormMethod? = null, action: String? = null, enctype: FormEnctype? = null,
            type: FormType? = null, condensed: Boolean = false,
            horizRatio: FormHorizontalRatio = FormHorizontalRatio.RATIO_2, classes: Set<String> = setOf(),
            customSerializers: Map<KClass<*>, KSerializer<*>>? = null,
            noinline init: (FormPanel<K>.() -> Unit)? = null
        ): FormPanel<K> {
            val formPanel =
                FormPanel(
                    method,
                    action,
                    enctype,
                    type,
                    condensed,
                    horizRatio,
                    classes,
                    serializer<K>(),
                    customSerializers
                )
            init?.invoke(formPanel)
            return formPanel
        }

    }
}

/**
 * DSL builder extension function.
 *
 * It takes the same parameters as the constructor of the built component.
 */
inline fun <reified K : Any> Container.formPanel(
    method: FormMethod? = null, action: String? = null, enctype: FormEnctype? = null,
    type: FormType? = null, condensed: Boolean = false,
    horizRatio: FormHorizontalRatio = FormHorizontalRatio.RATIO_2,
    classes: Set<String>? = null, className: String? = null,
    customSerializers: Map<KClass<*>, KSerializer<*>>? = null,
    noinline init: (FormPanel<K>.() -> Unit)? = null
): FormPanel<K> {
    val formPanel =
        create<K>(method, action, enctype, type, condensed, horizRatio, classes ?: className.set, customSerializers)
    init?.invoke(formPanel)
    this.add(formPanel)
    return formPanel
}

/**
 * DSL builder extension function.
 *
 * Simplified version of formPanel container without data model support.
 */
fun Container.form(
    method: FormMethod? = null, action: String? = null, enctype: FormEnctype? = null,
    type: FormType? = null, condensed: Boolean = false,
    horizRatio: FormHorizontalRatio = FormHorizontalRatio.RATIO_2,
    classes: Set<String>? = null, className: String? = null,
    init: (FormPanel<Any>.() -> Unit)? = null
): FormPanel<Any> {
    val formPanel =
        FormPanel<Any>(
            method,
            action,
            enctype,
            type,
            condensed,
            horizRatio,
            classes ?: className.set
        )
    init?.invoke(formPanel)
    this.add(formPanel)
    return formPanel
}
