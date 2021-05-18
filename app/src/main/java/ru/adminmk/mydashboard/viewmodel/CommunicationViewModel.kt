package ru.adminmk.mydashboard.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import ru.adminmk.mydashboard.model.api.DashboardFetcher
import ru.adminmk.mydashboard.model.api.DashboardResponse
import ru.adminmk.mydashboard.model.api.DataSet
import ru.adminmk.mydashboard.model.database.IndicatorsRepository
import ru.adminmk.mydashboard.model.database.OrderEntity
import ru.adminmk.mydashboard.model.subscribeToTopicInModel


class CommunicationViewModel : ViewModel() {
    private val TOPIC = "MyDashboard"

    var dashboardResponce: DashboardResponse? = null

    private lateinit var dashboardFetcher: DashboardFetcher
    private val sqlRepository = IndicatorsRepository.get()


    fun subscribeToTopic(failMessage: String, showOnComplete: (String) -> Unit) {
        subscribeToTopicInModel(TOPIC) { task ->
            if (!task.isSuccessful) {
                showOnComplete(failMessage)
            }
        }
    }


    fun createNotificationChannel(
        id: String, name: String,
        description: String, notificationManager: NotificationManager
    ) {


        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        channel.description = description

        notificationManager.createNotificationChannel(channel)
    }

    fun initFetchrWCredentials(
        login: String,
        password: String,
        serverAdress: String,
        dataBase: String
    ) {
//        val baseUrl ="http://10.10.10.36/1cMyDashboard/hs/Dashboard/"
        val baseUrl = "http://$serverAdress/$dataBase/hs/Dashboard/"
        dashboardFetcher = DashboardFetcher(login, password, baseUrl)
    }


    fun getDashboardData() = dashboardFetcher.createDashboardObservable()

    fun getOrderEntitiesList(): Single<List<OrderEntity>?> {

        return sqlRepository.getIndicators()
            .subscribeOn(Schedulers.io())
            .map(this::sortOrderEntities)
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun sortOrderEntities(orderEntities: List<OrderEntity>?): List<OrderEntity>? {
        return orderEntities?.sorted()
    }

    fun getUnValidatedPositionStart(orderEntities: List<OrderEntity>): Int {
        return orderEntities.indexOfFirst { it.isSubmitted == 0 }
    }


    fun checkNewIndicators(dashboardResponce: DashboardResponse): Single<DashboardResponse> {

        return sqlRepository.getAbsentOrRenamedIndicators(dashboardResponce.listOfIndicators)
            .subscribeOn(Schedulers.io())
            .flatMap { createAddNewIndicatorsObservable(it, dashboardResponce) }
            .observeOn(AndroidSchedulers.mainThread())

    }

    private fun createAddNewIndicatorsObservable(
        listOfIndicatorsToAddUpdate: List<OrderEntity>,
        dashboardResponce: DashboardResponse
    ): Single<DashboardResponse> {
        return Single.create { emitter ->
            try {

                listOfIndicatorsToAddUpdate.forEach {
                    if (it.isSubmitted == 0) {
                        sqlRepository.addIndicator(it)
                    } else {
                        sqlRepository.updateIndicator(it)
                    }
                }

                emitter.onSuccess(dashboardResponce)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

    }

    fun getSortOrderObservable(dashboardResponce: DashboardResponse): Single<DashboardResponse> {

        return createSortOrderObservable(dashboardResponce)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    private fun createSortOrderObservable(
        dashboardResponce: DashboardResponse
    ): Single<DashboardResponse> {
        return Single.create { emitter ->
            try {

                val sortedDashboardResponce = sortInputDataSet(dashboardResponce)

                emitter.onSuccess(sortedDashboardResponce)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }

    }

    private fun sortInputDataSet(dashboardResponce: DashboardResponse): DashboardResponse{
        val mapOfDataSet = HashMap<String, DataSet>()

        val listOfIds = dashboardResponce.listOfIndicators.map { val id = it.dataIndicator?.ID; mapOfDataSet[id!!] = it; id }.toList()
        val listOfOrderEntities =  sqlRepository.getSelectedIndicators(listOfIds)

        val sortedListOfIndicatorsInResponse = listOfOrderEntities?.let {
            val sortedOrderEntities = it.sorted()

            sortedOrderEntities.filter { it.isVisible == 1 }.map { orderEntity -> mapOfDataSet.get(orderEntity.id)}.filterNotNull().toList()
        } ?: emptyList()

        return dashboardResponce.copy(listOfIndicators = sortedListOfIndicatorsInResponse)
    }



    fun updateValidatedIndicator(indicator: OrderEntity, index: Int): Single<Unit> {
        return createUpdateValidatedIndicatorObservable(indicator, index)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    private fun createUpdateValidatedIndicatorObservable(indicator: OrderEntity, index: Int): Single<Unit> {
        return Single.create { emitter ->
            try {
                sqlRepository.updateIndicator(indicator)

                emitter.onSuccess(Unit)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }


    private fun updateIndicatorAsObservable(indicator: OrderEntity):Observable<OnIndicatorUpdateMessage> {
        val single: Single<OnIndicatorUpdateMessage> = Single.create { emitter ->
            try {

                sqlRepository.updateIndicator(indicator)

                emitter.onSuccess(OnIndicatorUpdateMessage())
            } catch (e: Exception) {
                emitter.onSuccess(OnIndicatorUpdateMessage(e))
            }

        }

        return single.toObservable().subscribeOn(Schedulers.io())
    }

    fun getUpdateIsVisibleIndicatorObservable(clickIsVisibleObservable: Observable<OrderEntity>):Observable<OnIndicatorUpdateMessage>{

        return clickIsVisibleObservable
            .switchMap(this::updateIndicatorAsObservable)
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun getUpdateOrderListObservable(listOfIndicatorsObservable: Observable<List<OrderEntity>>):Observable<OnIndicatorUpdateMessage>{
        return listOfIndicatorsObservable
            .switchMap(this::updateOrderOfListAsObservable)
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun updateOrderOfListAsObservable(listOfIndicators: List<OrderEntity>):Observable<OnIndicatorUpdateMessage> {
        val single: Single<OnIndicatorUpdateMessage> = Single.create { emitter ->
            try {

                listOfIndicators.forEach {
                    sqlRepository.updateIndicator(it)
                }

                emitter.onSuccess(OnIndicatorUpdateMessage())
            } catch (e: Exception) {
                emitter.onSuccess(OnIndicatorUpdateMessage(e))
            }

        }

        return single.toObservable().subscribeOn(Schedulers.io())
    }

    fun setIsVisibleConsistencyUpdateToUI(listOfIndicators: ArrayList<OrderEntity>, index: Int): OrderEntity{
        val currIndicator = listOfIndicators[index]
        val updatedIndicator = currIndicator.copy(isVisible = if (currIndicator.isVisible == 0) 1 else 0)
        listOfIndicators[index] =  updatedIndicator
        return updatedIndicator
    }

    fun setOrderConsistencyUpdateToUI(listOfIndicators: List<OrderEntity>)=
        listOfIndicators.mapIndexed { index, orderEntity ->  orderEntity.copy(orderValue = index)}.toList()

    fun setIsSubmitedConsistencyUpdateToUI(indicator: OrderEntity, index: Int): OrderEntity{
        val updatedIndicator = indicator.copy(isSubmitted = 1, orderValue = index)
        return updatedIndicator
    }

}