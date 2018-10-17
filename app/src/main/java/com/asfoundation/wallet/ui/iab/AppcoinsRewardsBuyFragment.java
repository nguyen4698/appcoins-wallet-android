package com.asfoundation.wallet.ui.iab;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.appcoins.wallet.bdsbilling.repository.entity.Purchase;
import com.appcoins.wallet.billing.BillingMessagesMapper;
import com.asf.wallet.BuildConfig;
import com.asf.wallet.R;
import com.asfoundation.wallet.repository.BdsPendingTransactionService;
import com.asfoundation.wallet.util.TransferParser;
import com.jakewharton.rxbinding2.view.RxView;
import dagger.android.support.DaggerFragment;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.math.BigDecimal;
import javax.inject.Inject;

public class AppcoinsRewardsBuyFragment extends DaggerFragment implements AppcoinsRewardsBuyView {

  public static final String AMOUNT_KEY = "amount";
  public static final String PACKAGE_NAME_KEY = "packageName";
  public static final String URI_KEY = "uri_key";
  public static final String PRODUCT_NAME = "product_name";
  public static final String IS_BDS = "is_bds";
  @Inject RewardsManager rewardsManager;
  @Inject BdsPendingTransactionService bdsPendingTransactionService;
  @Inject TransferParser transferParser;
  @Inject BillingMessagesMapper billingMessagesMapper;
  private View buyButton;
  private View cancelButton;
  private View loadingView;
  private View genericLoadingView;
  private AppcoinsRewardsBuyPresenter presenter;
  private TextView amountView;
  private TextView totalAmountView;
  private TextView productDescription;
  private BigDecimal amount;
  private View paymentDetailsView;
  private IabView iabView;
  private String uri;
  private TextView appName;
  private ImageView appIcon;
  private boolean isBds;

  public static Fragment newInstance(BigDecimal amount, String packageName, String uri,
      String productName, boolean isBds) {
    AppcoinsRewardsBuyFragment fragment = new AppcoinsRewardsBuyFragment();
    Bundle bundle = new Bundle();
    bundle.putString(AMOUNT_KEY, amount.toString());
    bundle.putString(PACKAGE_NAME_KEY, packageName);
    bundle.putString(URI_KEY, uri);
    bundle.putString(PRODUCT_NAME, productName);
    bundle.putBoolean(IS_BDS, isBds);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle arguments = getArguments();
    amount = new BigDecimal(arguments.getString(AMOUNT_KEY));
    uri = arguments.getString(URI_KEY);
    isBds = arguments.getBoolean(IS_BDS);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.reward_payment_layout, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    buyButton = view.findViewById(R.id.buy_button);
    cancelButton = view.findViewById(R.id.cancel_button);
    loadingView = view.findViewById(R.id.loading_view);
    genericLoadingView = view.findViewById(R.id.loading);
    appName = view.findViewById(R.id.app_name);
    amountView = view.findViewById(R.id.sku_price);
    totalAmountView = view.findViewById(R.id.total_price);
    productDescription = view.findViewById(R.id.sku_description);
    paymentDetailsView = view.findViewById(R.id.info_dialog);
    appIcon = view.findViewById(R.id.app_icon);

    presenter =
        new AppcoinsRewardsBuyPresenter(this, rewardsManager, AndroidSchedulers.mainThread(),
            new CompositeDisposable(), amount, BuildConfig.DEFAULT_STORE_ADDRESS,
            BuildConfig.DEFAULT_OEM_ADDRESS, uri, getCallerPackageName(), transferParser,
            getProductName(), isBds);

    Single.defer(() -> Single.just(getCallerPackageName()))
        .observeOn(Schedulers.io())
        .map(packageName -> new Pair<>(getApplicationName(packageName),
            getContext().getPackageManager()
                .getApplicationIcon(packageName)))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pair -> {
          appName.setText(pair.first);
          appIcon.setImageDrawable(pair.second);
        }, throwable -> {
          throwable.printStackTrace();
        });
  }

  @Override public void onStart() {
    super.onStart();
    presenter.present();
  }

  @Override public void onStop() {
    presenter.stop();
    super.onStop();
  }

  private CharSequence getApplicationName(String appPackage)
      throws PackageManager.NameNotFoundException {
    PackageManager packageManager = getContext().getPackageManager();
    ApplicationInfo packageInfo = packageManager.getApplicationInfo(appPackage, 0);
    packageInfo.loadIcon(getContext().getPackageManager());
    appIcon.setImageResource(R.drawable.appbar_background_color);
    return packageManager.getApplicationLabel(packageInfo);
  }

  private String getProductName() {
    return getArguments().getString(PRODUCT_NAME, "");
  }

  private String getCallerPackageName() {
    return getArguments().getString(PACKAGE_NAME_KEY);
  }

  @Override public Observable<Object> getBuyClick() {
    return RxView.clicks(buyButton);
  }

  @Override public void showProcessingLoading() {
    loadingView.setVisibility(View.VISIBLE);
    paymentDetailsView.setVisibility(View.INVISIBLE);
  }

  @Override public void setupView(String amount, String productName, String packageName) {
    amountView.setText(String.format("%s APPC Rewards", amount));
    totalAmountView.setText(String.format("%s APPC Rewards", amount));
    productDescription.setText(productName);
    loadingView.setVisibility(View.GONE);
  }

  @Override public void showPaymentDetails() {
    loadingView.setVisibility(View.GONE);
    paymentDetailsView.setVisibility(View.VISIBLE);
  }

  @Override public void hidePaymentDetails() {
    paymentDetailsView.setVisibility(View.GONE);
  }

  @Override public void finish(Purchase purchase) {
    iabView.finish(billingMessagesMapper.mapPurchase(purchase));
  }

  @Override public void hideGenericLoading() {
    loadingView.setVisibility(View.GONE);
  }

  @Override public void showLoading() {
    genericLoadingView.setVisibility(View.VISIBLE);
  }

  @Override public void hideLoading() {
    genericLoadingView.setVisibility(View.GONE);
  }

  @Override public void showNoNetworkError() {
    Toast.makeText(getContext(), "network error", Toast.LENGTH_SHORT)
        .show();
  }

  @Override public Observable<Object> getCancelClick() {
    return RxView.clicks(cancelButton);
  }

  @Override public void close() {
    Bundle bundle = new Bundle();
    bundle.putInt("RESPONSE_CODE", Activity.RESULT_OK);
    iabView.close(bundle);
  }

  @Override public void showGenericError() {
    Toast.makeText(getContext(), "Generic Error", Toast.LENGTH_SHORT)
        .show();
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof IabView)) {
      throw new IllegalStateException(
          "Express checkout buy fragment must be attached to IAB activity");
    }
    iabView = ((IabView) context);
  }
}
