import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class CrimesObserver implements Observer<CrimesMap> {
    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(CrimesMap crimesMap) {
        System.out.println("Got crimes map with size "+crimesMap.size());
    }

    @Override
    public void onError(Throwable e) {
        System.out.println("Error");

    }

    @Override
    public void onComplete() {
        System.out.println("Completed");
    }
}
