package ru.adminmk.mydashboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.adminmk.mydashboard.R
import ru.adminmk.mydashboard.databinding.MainFragmentBinding
import ru.adminmk.mydashboard.MainCallbacks
import ru.adminmk.mydashboard.viewmodel.*

class MainFragment : Fragment() {
    private var binding: MainFragmentBinding? = null


    private val viewModel: MainViewModel by activityViewModels()
    private val communicationViewModel: CommunicationViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }


    private fun setupAccessibilityObservers(mainFragmentBinding: MainFragmentBinding) {

        viewModel.loginInProgress.observe(this.viewLifecycleOwner, { isInProgress ->
            mainFragmentBinding.editTextServerAdress.isEnabled = !isInProgress
            mainFragmentBinding.editTextDataBase.isEnabled = !isInProgress
            mainFragmentBinding.editTextLogin.isEnabled = !isInProgress
            mainFragmentBinding.editTextPassword.isEnabled = !isInProgress
            mainFragmentBinding.editTextID.isEnabled = !isInProgress

            if (isInProgress) {
                mainFragmentBinding.loginButtonTextView.setText(R.string.connect_button_stop)
                mainFragmentBinding.loginButton.visibility = View.GONE
                mainFragmentBinding.progressBar.visibility = View.VISIBLE
            } else {
                mainFragmentBinding.loginButtonTextView.setText(R.string.connect_button)
                mainFragmentBinding.loginButton.visibility = View.VISIBLE
                mainFragmentBinding.progressBar.visibility = View.GONE
            }
        })

        viewModel.logedIn.observe(this.viewLifecycleOwner, { loginState ->
            loginState?.let {
                if (it.isLogedIn == true) {
                    val mainCallbaks = this.activity as MainCallbacks

                    mainCallbaks.onLogin(it.dashboardResponse)
                }
            }
        })

        viewModel.loginError.observe(this.viewLifecycleOwner, { loginError ->
            loginError?.let {
                val mainCallbaks = this.activity as MainCallbacks

                val stringError = mainCallbaks.composeError(it)

                if (it.isDetected) {
                    mainFragmentBinding.textViewError.visibility = View.VISIBLE
                    mainFragmentBinding.textViewError.text = stringError
                } else {
                    mainFragmentBinding.textViewError.visibility = View.GONE
                }

                if (it.isServerAdressBlank) {
                    mainFragmentBinding.editTextServerAdress.error =
                        getString(R.string.server_adress_error)
                } else {
                    mainFragmentBinding.editTextServerAdress.error = null
                }
                if (it.isDataBaseBlank) {
                    mainFragmentBinding.editTextDataBase.error = getString(R.string.data_base_error)
                } else {
                    mainFragmentBinding.editTextDataBase.error = null
                }
                if (it.isLoginBlank) {
                    mainFragmentBinding.editTextLogin.error = getString(R.string.login_error)
                } else {
                    mainFragmentBinding.editTextLogin.error = null
                }
                if (it.isPasswordBlank) {
                    mainFragmentBinding.editTextPassword.error = getString(R.string.password_error)
                } else {
                    mainFragmentBinding.editTextPassword.error = null
                }
                if (it.isIDBlank) {
                    mainFragmentBinding.editTextID.error = getString(R.string.id_error)
                } else {
                    mainFragmentBinding.editTextID.error = null
                }
            }
        })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainFragmentBinding = binding as MainFragmentBinding

        setButtonListeners(mainFragmentBinding)
        initTextValues(mainFragmentBinding)
        setupAccessibilityObservers(mainFragmentBinding)
    }

    private fun initTextValues(_binding: MainFragmentBinding) {
        _binding.editTextServerAdress.setText(viewModel.serverAdress ?: "")
        _binding.editTextDataBase.setText(viewModel.dataBase ?: "")
        _binding.editTextLogin.setText(viewModel.login ?: "")
        _binding.editTextPassword.setText(viewModel.password ?: "")
        _binding.editTextID.setText(viewModel.id ?: "")

        val sharedPreferences = context?.getSharedPreferences(LOGIN_SETTINGS, Context.MODE_PRIVATE)
        val actionOnDataChange: (String, String) -> Unit = { field: String, value: String ->
            viewModel.saveLoginData(
                field,
                value,
                sharedPreferences!!
            ); viewModel.setCredentiaHaveChanged()
        }

        _binding.editTextServerAdress.afterTextChanged(
            actionOnDataChange,
            MainViewModel.SERVER_ADRESS
        )
        _binding.editTextDataBase.afterTextChanged(actionOnDataChange, MainViewModel.DATABASE)
        _binding.editTextLogin.afterTextChanged(actionOnDataChange, MainViewModel.LOGIN)
        _binding.editTextPassword.afterTextChanged(actionOnDataChange, MainViewModel.PASSWORD)
        _binding.editTextID.afterTextChanged(actionOnDataChange, MainViewModel.ID)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButtonListeners(_binding: MainFragmentBinding) {
        val gestureListener: (View, MotionEvent) -> Boolean = { _, m: MotionEvent ->
            handleTouch(m)
            true
        }


        _binding.buttonFrame.setOnTouchListener(gestureListener)
        _binding.loginButton.setOnTouchListener(gestureListener)
    }

    private fun handleTouch(m: MotionEvent) {
        val mainFragmentBinding = binding as MainFragmentBinding
        val pointerCount = m.pointerCount

        val effect = VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE)

        for (i in 0 until pointerCount) {


            when (m.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val picR = R.drawable.ic_power_on_enabled


                    val textColorDown = if (viewModel.loginInProgress.value == true) {
                        R.color.red_700
                    } else {
                        R.color.blue_200
                    }

                    val fontR = R.font.roboto_bold
                    markButton(mainFragmentBinding, picR, textColorDown, fontR, effect)
                }
                MotionEvent.ACTION_UP -> {
                    val picR = R.drawable.ic_power_on
                    val textColorUp = R.color.text_default
                    val fontR = R.font.roboto
                    markButton(mainFragmentBinding, picR, textColorUp, fontR, effect)

                    viewModel.onLoginPressed(context, communicationViewModel)
                }
            }
        }
    }


    private fun markButton(
        _binding: MainFragmentBinding,
        picR: Int,
        textColorR: Int,
        fontR: Int,
        effect: VibrationEffect
    ) {
        val drawable = ContextCompat.getDrawable(requireContext(), picR) ?: ColorDrawable()
        _binding.loginButton.background = drawable

        val color = ContextCompat.getColor(requireContext(), textColorR)
        _binding.loginButtonTextView.setTextColor(color)

        val typeface = resources.getFont(fontR)
        _binding.loginButtonTextView.typeface = typeface

        val mainCallbacks = this.activity as MainCallbacks
        mainCallbacks.vibrate(effect)
    }


    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }


    override fun onResume() {
        super.onResume()

        viewModel.titleFragmentOnResume(R.string.app_name_login)
    }

    override fun onStart() {
        super.onStart()


        viewModel.setCurrentFragment(CurrentFragment.LOGIN)
    }
}


fun EditText.afterTextChanged(afterTextChanged: (String, String) -> Unit, field: String) {

    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(field, editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

