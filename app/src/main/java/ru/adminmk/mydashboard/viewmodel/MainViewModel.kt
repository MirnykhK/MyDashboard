package ru.adminmk.mydashboard.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import ru.adminmk.mydashboard.R
import ru.adminmk.mydashboard.model.api.DashboardResponse
import ru.adminmk.mydashboard.model.hasNetwork
import ru.adminmk.mydashboard.model.saveCredentialsInModel


private const val TAG="MainViewModel"
private const val IS_LOGED_IN = "curFragment"
private const val DASHBOARD_RESPONSE = "DBResponse"
const val LOGIN_SETTINGS = "login_settings"

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _logedIn = MutableLiveData<LoginState>()
    val logedIn: LiveData<LoginState> = _logedIn

    private val _loginInProgress = MutableLiveData<Boolean>()
    val loginInProgress: LiveData<Boolean> = _loginInProgress

    private val _loginError = MutableLiveData<LoginError>()
    val loginError: LiveData<LoginError> = _loginError

    private val _currFragment = MutableLiveData<CurrentFragment>()
    val currFragment: LiveData<CurrentFragment> = _currFragment

    companion object{
        const val SERVER_ADRESS = "ServerAdress"
        const val DATABASE = "DataBase"
        const val LOGIN = "Login"
        const val PASSWORD = "Password"
        const val ID = "ID"
        const val SETTINGS_AUTOLOGIN = "AutoLogin"
        const val SETTINGS_NOTIFICATION_MODE = "NotificationMode"

        val BLANK_ERROR = LoginError()
    }

    var serverAdress: String? = null
        private set
    var dataBase: String? = null
        private set
    var login: String? = null
        private set
    var password: String? = null
        private set
    var id: String? = null
        private set
    var autologin: Boolean = false
        private set
    var notificationMode: NotificationMode = NotificationMode.SILENT
        private set

    private var responseSubscription = CompositeDisposable()


    private val subjectTitle = BehaviorSubject.createDefault(R.string.app_name_login)
    private val subjectTitleConnectingInProcess = PublishSubject.create<Int>()

    private val subjectFetchrInitializer = BehaviorSubject.createDefault(false)
    private val subjectStartLoginEvents = PublishSubject.create<CommunicationViewModel>()



    init {
        _loginError.value = BLANK_ERROR
        if (savedStateHandle.contains(IS_LOGED_IN)) {

            _logedIn.value = LoginState(
                savedStateHandle.get(IS_LOGED_IN),
                savedStateHandle.get(DASHBOARD_RESPONSE)
            )
        }

        initLoginChain()
    }


    private fun initLoginChain(){
        val loginSubscription =  subjectStartLoginEvents.withLatestFrom(
            subjectFetchrInitializer,
            { communicationViewModel, isCredentialsValid -> Pair(communicationViewModel,isCredentialsValid)}).subscribe(this::startLoginWInitializer)
    }

    fun initCredentials(serverAdress: String?, dataBase: String?, login: String?, password: String?, id: String?, autologin: Boolean, notificationMode: Int){
        this.serverAdress = serverAdress
        this.dataBase = dataBase
        this.login = login
        this.password = password
        this.id = id
        this.autologin = autologin
        this.notificationMode = NotificationMode.values()[notificationMode]
    }

    fun saveLoginData(field: String, value: Any?, sharedPreferences: SharedPreferences){
        saveCredentialsInModel(sharedPreferences, field, value)

        when(field){
            SERVER_ADRESS -> serverAdress =  value as String?
            DATABASE -> dataBase =  value as String?
            LOGIN -> login =  value as String?
            PASSWORD -> password =  value as String?
            ID -> id =  value as String?
            SETTINGS_AUTOLOGIN -> autologin =  value as Boolean
            SETTINGS_NOTIFICATION_MODE -> notificationMode =  NotificationMode.values()[value as Int]
        }
    }

    fun onLoginPressed(context: Context?, communicationViewModel:CommunicationViewModel){
        return when(_loginInProgress.value){
                true -> {stopLogin()}
                false -> {startLogin(context, communicationViewModel)}
                else -> {startLogin(context, communicationViewModel) }
            }
    }

    private fun startLogin(context: Context?, communicationViewModel:CommunicationViewModel, dataValidationIsPassed:Boolean= false){

        if(!dataValidationIsPassed){
            val blankDataError = checkForData()

            blankDataError?.let {
                _loginError.value = it
                return
            }
        }


        val connectionError =  checkInternetConnection(context)
        connectionError?.let {
            _loginError.value = it
            return
        }

        loginObservableEvent(communicationViewModel)

    }

    private fun checkForData(): LoginError? {
//        val errorMessageBuilder = StringBuilder()

        val listOfErrors = ArrayList<Int>()

        val isServerAdressBlank = serverAdress?.isBlank() ?: true
        val isDataBaseBlank = dataBase?.isBlank() ?: true
        val isLoginBlank = login?.isBlank() ?: true
        val isPasswordBlank = password?.isBlank() ?: true
        val isIDBlank = id?.isBlank() ?: true

        if (isServerAdressBlank) listOfErrors.add(R.string.server_adress_error)
        if (isDataBaseBlank) listOfErrors.add(R.string.data_base_error)
        if (isLoginBlank) listOfErrors.add(R.string.login_error)
        if (isPasswordBlank) listOfErrors.add(R.string.password_error)
        if (isIDBlank) listOfErrors.add(R.string.id_error)


        return if (listOfErrors.isEmpty()) {
            null
        } else {


            LoginError(
                listOfErrors,
                true,
                isServerAdressBlank,
                isDataBaseBlank,
                isLoginBlank,
                isPasswordBlank,
                isIDBlank
            )
        }

    }

    private fun stopLogin(){
        unsubscribeToResponce(true)
    }


    fun getStartFragment(): Int{
        if(!autologin) return R.id.mainFragment

        val loginError = checkForData()

        return if (loginError == null) {
            R.id.performanceFragment
        } else {
            R.id.mainFragment
        }
    }

    private fun checkInternetConnection(context: Context?): LoginError?{
        if(hasNetwork(context)){
            return null
        }

        return LoginError(listOfErrors = listOf(R.string.web_error), isDetected = true)
    }

    // Activity Title Observable:
    fun getTitleObservable(): Observable<Int> {
        return subjectTitle.mergeWith (subjectTitleConnectingInProcess)
    }

    fun titleFragmentOnResume(title: Int) {
        subjectTitle.onNext(title)
    }

    private fun titleLoginInProgress(isInProgress: Boolean) {
        if (isInProgress) {
            subjectTitleConnectingInProcess.onNext(R.string.app_name_login_in_progress)
        } else {
            subjectTitle.value?.let {
                subjectTitle.onNext(it)
            }
        }
    }


    //Fetchr initializing logic:
    private fun loginObservableEvent(communicationViewModel:CommunicationViewModel){
        subjectStartLoginEvents.onNext(communicationViewModel)
    }

    fun setCredentiaHaveChanged(){
        subjectFetchrInitializer.onNext(false)
    }

    private fun startLoginWInitializer(initializerParameters:Pair<CommunicationViewModel, Boolean>){
        _loginInProgress.value=true
        titleLoginInProgress(true)
        _loginError.value = BLANK_ERROR

        val communicationViewModel = initializerParameters.first
        val isCredentialsValid = initializerParameters.second

        if(!isCredentialsValid){
            communicationViewModel.initFetchrWCredentials(login!!, password!!, serverAdress!!, dataBase!!)
            subjectFetchrInitializer.onNext(true)
        }

        responseSubscription.let {
            if(!it.isDisposed) it.dispose()

            responseSubscription = CompositeDisposable()
        }

        val actionOnError: (e:Throwable) -> Unit = {e:Throwable->
            _loginInProgress.value = false
            titleLoginInProgress(false)
            _loginError.value = LoginError(null, true, throwableError = e.message)
            _logedIn.value = LoginState(false)

            Log.d(TAG, "error: ${e.message}")
        }


        val actionOnNextInetResponse: (it:DashboardResponse?) -> Unit = {
            it?.let {
//                logInputMessage(it)

                val absentOrRenamedIndicatorsObservable = communicationViewModel.checkNewIndicators(it)

                val actionOnNextAbsentOrRenamedIndicators: (it: DashboardResponse) -> Unit = {

                    savedStateHandle.set(IS_LOGED_IN, true)
                    savedStateHandle.set(DASHBOARD_RESPONSE, it)

                    _loginInProgress.value = false
                    titleLoginInProgress(false)
                    _logedIn.value = LoginState(true, it)

                }

                responseSubscription.add(absentOrRenamedIndicatorsObservable.subscribe(actionOnNextAbsentOrRenamedIndicators, actionOnError))
            }

        }





        val communicationObservableResult=  communicationViewModel.getDashboardData()



        responseSubscription.add(
            communicationObservableResult.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(actionOnNextInetResponse, actionOnError))

    }

    fun unsubscribeToResponce(onStop: Boolean = false){
        val isSubscriptonAlive = responseSubscription.let { !it.isDisposed }
        if(onStop || isSubscriptonAlive) {
            if(isSubscriptonAlive) responseSubscription.dispose()

            _loginInProgress.value = false
            _loginError.value = BLANK_ERROR
        }
    }

//    private fun logInputMessage(response: DashboardResponse){
//        Log.d(TAG, "time: ${response.calcTime}")
//        for (dataSet in response.listOfIndicators) {
//            Log.d(
//                TAG,
//                "${dataSet.dataIndicator?.name} ${dataSet.dataIndicator?.ID} ${dataSet.value} ${dataSet.direction}"
//            )
//        }
//    }

    fun setCurrentFragment(currentFragment: CurrentFragment){
        _currFragment.postValue(currentFragment)
    }
}

