package sai.application.betch.alerts.create_alert;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import sai.application.betch.alerts.AlertsActivityMVP;
import sai.application.betch.analytics.FirebaseHelper;
import sai.application.betch.cache.cachemodel.Alert;
import sai.application.betch.events.AlertSavedEvent;
import sai.application.betch.home.CurrencyViewModel;
import timber.log.Timber;

import static sai.application.betch.alerts.create_alert.Constants.ONE_DAY_ALARM_IN_MINS;
import static sai.application.betch.alerts.create_alert.Constants.ONE_DAY_ALARM_KEY;
import static sai.application.betch.alerts.create_alert.Constants.ONE_HOUR_ALARM_KEY;
import static sai.application.betch.alerts.create_alert.Constants.PRICE_TRIGGER;
import static sai.application.betch.alerts.create_alert.Constants.SIX_HOUR_ALARM_IN_MINS;
import static sai.application.betch.alerts.create_alert.Constants.SIX_HOUR_ALARM_KEY;
import static sai.application.betch.alerts.create_alert.Constants.TIME_TRIGGER;
import static sai.application.betch.alerts.create_alert.Constants.TWELVE_HOUR_ALARM_IN_MINS;
import static sai.application.betch.alerts.create_alert.Constants.TWELVE_HOUR_ALARM_KEY;
import static sai.application.betch.alerts.create_alert.Constants.TWO_HOUR_ALARM_IN_MINS;
import static sai.application.betch.alerts.create_alert.Constants.TWO_HOUR_ALARM_KEY;

/**
 * Created by sai on 7/19/17.
 */

public class CreateAlertPresenter implements CreateAlertMVP.Presenter {

    private AlertsActivityMVP.Model model;
    private CreateAlertMVP.View view;

    private CompositeDisposable mNetworkSisposables = new CompositeDisposable();
    private CompositeDisposable mEventDisposables = new CompositeDisposable();

    private CurrencyViewModel mSelectedCurrencyViewModel = null;
    private AlertTime mSelectedAlertTime = null;
    private String mTriggerType = null;
    private String mPriceTrigger = null;
    private boolean isDataValid = false;

    private FirebaseHelper mFirebaseHelper;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, y", Locale.US);

    public CreateAlertPresenter(AlertsActivityMVP.Model model, FirebaseHelper firebaseHelper) {
        this.model = model;
        this.mFirebaseHelper = firebaseHelper;
    }

    @Override
    public void closeDialogClicked() {
        view.closeView();
    }

    @Override
    public void loadCurrencyData() {
        Disposable currencyDisposable = model.currencyData().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<CurrencyViewModel>() {
                    @Override
                    public void onNext(CurrencyViewModel viewModel) {
                        Timber.d("Fetched currency data for creating alerts");
                        view.updateCurrencyData(viewModel);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {
                        Timber.d("Completed fetching currency data for creating alerts");
                    }
                });
        mNetworkSisposables.add(currencyDisposable);
    }

    @Override
    public void rxUnsubscribe() {
        mNetworkSisposables.clear();
    }

    @Override
    public void rxDestroy() {
        mNetworkSisposables.dispose();
    }

    @Override
    public void setView(CreateAlertMVP.View view) {
        this.view = view;
    }

    @Override
    public void createAlertClicked() {

    }

    @Override
    public void handleCurrencySelected(Observable<CurrencyViewModel> observable) {
        DisposableObserver<CurrencyViewModel> currencyDisposable = new DisposableObserver<CurrencyViewModel>() {
            @Override
            public void onNext(CurrencyViewModel viewModel) {
                Timber.d("User selecred currency: " + viewModel.toString());
                mSelectedCurrencyViewModel = viewModel;
                validateData();

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(currencyDisposable);

        mEventDisposables.add(currencyDisposable);
    }

    @Override
    public void handleTriggerTypeSelected(Observable<String> observable) {
        DisposableObserver<String> triggerTypeDisposable = new DisposableObserver<String>() {
            @Override
            public void onNext(String s) {
                Timber.d("Trigger type selected : " + s);
                mTriggerType = s;
                view.toggleTriggerLayouts(mTriggerType.equalsIgnoreCase(PRICE_TRIGGER));
                validateData();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(triggerTypeDisposable);
        mEventDisposables.add(triggerTypeDisposable);
    }

    @Override
    public void handlePriceEntered(Observable<String> observable) {
        DisposableObserver<String> priceDisposable = new DisposableObserver<String>() {
            @Override
            public void onNext(String s) {
                Timber.d("Price entered : " + s);
                mPriceTrigger = s;
                validateData();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(priceDisposable);
        mEventDisposables.add(priceDisposable);
    }

    @Override
    public void handleAlertTimeSelected(Observable<AlertTime> observable) {
        DisposableObserver<AlertTime> timeAlertDisposable = new DisposableObserver<AlertTime>() {
            @Override
            public void onNext(AlertTime alertTime) {
                Timber.d("Time entered : " + alertTime.getTimeString());
                mSelectedAlertTime = alertTime;
                validateData();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(timeAlertDisposable);
        mEventDisposables.add(timeAlertDisposable);
    }

    @Override
    public void handleCreateAlertClicked(Observable observable) {
        DisposableObserver<Boolean> saveObserver = new DisposableObserver<Boolean>() {
            @Override
            public void onNext(Boolean o) {
                 Alert alert = new Alert(generateGUID());

                alert.setActive(true);
                alert.setCurrencyName(mSelectedCurrencyViewModel.getCurrencyName());
                alert.setCurrencySymbol(mSelectedCurrencyViewModel.getCurrencySymbol());
                alert.setCreatedDate(getCurrentDate());
                boolean isPriceTrigger = mPriceTrigger != null;
                alert.setTriggerUnit(isPriceTrigger ? Double.parseDouble(mPriceTrigger) : mSelectedAlertTime.getTimeValue());
                alert.setTimeTrigger(!isPriceTrigger);
                alert.setLessThan(true);
                if(isPriceTrigger) {
                    double triggerPrice = Double.parseDouble(mPriceTrigger);
                    if(triggerPrice > Double.parseDouble(mSelectedCurrencyViewModel.getCostPerUnit())) {
                        alert.setLessThan(false);
                    }
                }
                alert.setCurrencyId(mSelectedCurrencyViewModel.getId());

                model.saveAlert(alert).subscribe();
                if(!isPriceTrigger) {
                    scheduleJobIfNecessary(alert);
                }
                Timber.d("Saving alert done! ");
                mFirebaseHelper.logAlertCreatedEvent(alert);
                view.showMessage("Alert saved!");
                view.closeView();
                EventBus.getDefault().post(new AlertSavedEvent());
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {}
        };

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(saveObserver);


        mEventDisposables.add(saveObserver);
    }

    private void scheduleJobIfNecessary(Alert alert) {
        boolean shouldStartJob;
        long alarmMinutes;
        if(alert.getTriggerUnit() == 60) {
            shouldStartJob = !model.getBoolean(ONE_HOUR_ALARM_KEY, false);
            model.saveBoolean(ONE_HOUR_ALARM_KEY, true);
            alarmMinutes = Constants.ONE_HOUR_ALARM_IN_MINS;
        }
        else if(alert.getTriggerUnit() == 120) {
            shouldStartJob = !model.getBoolean(TWO_HOUR_ALARM_KEY, false);
            model.saveBoolean(TWO_HOUR_ALARM_KEY, true);
            alarmMinutes = TWO_HOUR_ALARM_IN_MINS;
        }
        else if(alert.getTriggerUnit() == 360) {
            shouldStartJob = !model.getBoolean(SIX_HOUR_ALARM_KEY, false);
            model.saveBoolean(SIX_HOUR_ALARM_KEY, true);
            alarmMinutes = SIX_HOUR_ALARM_IN_MINS;
        }
        else if(alert.getTriggerUnit() == 720) {
            shouldStartJob = !model.getBoolean(TWELVE_HOUR_ALARM_KEY, false);
            model.saveBoolean(TWELVE_HOUR_ALARM_KEY, true);
            alarmMinutes = TWELVE_HOUR_ALARM_IN_MINS;
        }
        else {
            shouldStartJob = !model.getBoolean(ONE_DAY_ALARM_KEY, false);
            model.saveBoolean(ONE_DAY_ALARM_KEY, true);
            alarmMinutes = ONE_DAY_ALARM_IN_MINS;
        }

        if(shouldStartJob)
        view.startTimeAlarm(alarmMinutes);
    }

    private String getCurrentDate() {
        return simpleDateFormat.format(new Date(System.currentTimeMillis()));
    }

    private String generateGUID() {
        return UUID.randomUUID().toString();
    }

    private void validateData() {
        if(mSelectedCurrencyViewModel == null) {
            isDataValid = false;
            view.toggleCreateAlertButtonEnabled(false);
            return;
        }

        if(mTriggerType == null) {
            isDataValid = false;
            view.toggleCreateAlertButtonEnabled(false);
            return;
        }

        if(mTriggerType.equals(PRICE_TRIGGER) && (mPriceTrigger == null || !isValidNumber(mPriceTrigger))) {
            isDataValid = false;
            view.toggleCreateAlertButtonEnabled(false);
            return;
        }
        else if(mTriggerType.equals(TIME_TRIGGER) && mSelectedAlertTime == null) {
            isDataValid = false;
            view.toggleCreateAlertButtonEnabled(false);
            return;
        }

        isDataValid = true;
        view.toggleCreateAlertButtonEnabled(true);
    }

    private boolean isValidNumber(String price) {
        try {
            Double.parseDouble(price);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
