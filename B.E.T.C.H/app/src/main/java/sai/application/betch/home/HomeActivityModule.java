package sai.application.betch.home;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import sai.application.betch.network.CryptoCurrencyApiService;
import sai.application.betch.repository.IRepository;
import sai.application.betch.repository.Repository;

/**
 * Created by sai on 7/16/17.
 */

@Module
public class HomeActivityModule {

    @Provides
    public HomeActivityMVP.Presenter provideHomeActivityPresenter(HomeActivityMVP.Model model) {
        return new HomeActivityPresenter(model);
    }

    @Provides
    public HomeActivityMVP.Model provideHomeActivityModel(IRepository repository) {
        return new HomeActivityModel(repository);
    }

    @Singleton
    @Provides
    IRepository provideRepository(CryptoCurrencyApiService apiService) {
        return new Repository(apiService);
    }
}
