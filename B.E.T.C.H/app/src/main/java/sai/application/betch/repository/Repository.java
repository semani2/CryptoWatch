package sai.application.betch.repository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import sai.application.betch.cache.IAlertsCacheService;
import sai.application.betch.cache.ICurrencyCacheService;
import sai.application.betch.cache.cachemodel.Alert;
import sai.application.betch.cache.cachemodel.Currency;
import sai.application.betch.network.CryptoCurrencyApiService;
import sai.application.betch.network.apimodel.CryptoCurrency;
import sai.application.betch.sharedpreferences.ISharedPreferenceService;
import timber.log.Timber;

/**
 * Created by sai on 7/16/17.
 */

public class Repository implements IRepository {

    private CryptoCurrencyApiService mCryptoCurrencyApiService;
    private IAlertsCacheService mAlertsCacheService;
    private ISharedPreferenceService mSharedPreferenceService;
    private ICurrencyCacheService mCurrencyCacheService;

    private long mTimeStamp;

    private static final long STALE_MS = 15 * 1000;

    public Repository(CryptoCurrencyApiService apiService, IAlertsCacheService alertsCacheService,
                      ISharedPreferenceService sharedPreferenceService, ICurrencyCacheService currencyCacheService) {
        this.mCryptoCurrencyApiService = apiService;
        this.mAlertsCacheService = alertsCacheService;
        this.mSharedPreferenceService = sharedPreferenceService;
        this.mCurrencyCacheService = currencyCacheService;
        mTimeStamp = 0;
    }

    private boolean isDataUptoDate() {
        return System.currentTimeMillis() - mTimeStamp < STALE_MS;
    }

    @Override
    public Observable<List<CryptoCurrency>> loadDataFromNetwork(int limit) {
        return mCryptoCurrencyApiService.getCurrencyData(limit)
                .doOnNext(new Consumer<List<CryptoCurrency>>() {
                    @Override
                    public void accept(@NonNull List<CryptoCurrency> cryptoCurrencies) throws Exception {
                        List<Currency> currencies = new ArrayList<>();
                        for(CryptoCurrency cryptoCurrency : cryptoCurrencies) {
                            currencies.add(new Currency(cryptoCurrency));
                        }

                        mCurrencyCacheService.saveAllCurrencies(currencies);
                    }
                });
    }

    @Override
    public Observable<List<CryptoCurrency>> loadDataFromCache() {
        if(mTimeStamp != 0 && isDataUptoDate()) {
            return mCurrencyCacheService.getCurrencyFromCache().concatMap(new Function<List<Currency>, ObservableSource<List<CryptoCurrency>>>() {
                @Override
                public ObservableSource<List<CryptoCurrency>> apply(@NonNull List<Currency> currencies) throws Exception {
                    List<CryptoCurrency> cryptoCurrencies = new ArrayList<>();

                    for(Currency currency : currencies) {
                        cryptoCurrencies.add(new CryptoCurrency(currency));
                    }

                    return Observable.just(cryptoCurrencies);
                }
            });
        }
        else {
            mTimeStamp = System.currentTimeMillis();
            return Observable.empty();
        }
    }

    @Override
    public Observable<List<CryptoCurrency>> loadData() {
        return loadDataFromCache().switchIfEmpty(loadDataFromNetwork(20));
    }

    @Override
    public Observable<Alert> loadAlertsFromCache() {
        return mAlertsCacheService.getAlertsFromCache();
    }

    @Override
    public Observable saveAlert(Alert alert) {
        return mAlertsCacheService.saveAlert(alert);
    }

    @Override
    public Observable<Alert> getAlert(String guid) {
        return mAlertsCacheService.getAlert(guid);
    }

    @Override
    public Observable deleteAlert(String guid) {
        return mAlertsCacheService.deleteAlert(guid);
    }

    @Override
    public Observable updateAlertIsActive(String guid, final boolean isActive) {
        getAlert(guid).subscribeWith(new DisposableObserver<Alert>() {
            @Override
            public void onNext(Alert alert) {
                alert.setActive(isActive);
                saveAlert(alert).subscribe();
                Timber.d("Alert active state changed");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        return Observable.empty();
    }

    @Override
    public Observable<Alert> getActivePriceAlerts() {
        return mAlertsCacheService.getActivePriceAlerts();
    }

    @Override
    public Observable<Alert> getActiveTimeAlerts(long minutes) {
        return mAlertsCacheService.getActiveTimeAlerts(minutes);
    }

    @Override
    public Observable<Integer> getTotalAlerts() {
        return mAlertsCacheService.getTotalAlerts();
    }

    @Override
    public void saveBoolean(String key, boolean value) {
        mSharedPreferenceService.saveBoolean(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferenceService.getBoolean(key, defValue);
    }
}
