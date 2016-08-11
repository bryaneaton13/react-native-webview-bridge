package com.github.alinz.reactnativewebviewbridge;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.views.webview.ReactWebViewManager;
import com.facebook.react.views.webview.WebViewConfig;


import android.annotation.TargetApi;
import android.webkit.PermissionRequest;
import android.os.Build;

import java.util.Map;

import javax.annotation.Nullable;

public class WebViewBridgeManager extends ReactWebViewManager {
  private static final String REACT_CLASS = "RCTWebViewBridge";

  public static final int COMMAND_INJECT_BRIDGE_SCRIPT = 100;
  public static final int COMMAND_SEND_TO_BRIDGE = 101;

  public WebViewBridgeManager() {
    super(new WebViewConfig() {
      @Override
      public void configWebView(WebView webView) {
        JavascriptBridge jsInterface = new JavascriptBridge((ReactContext)webView.getContext());
        webView.addJavascriptInterface(jsInterface, "WebViewBridgeAndroid");
        webView.setWebChromeClient(new WebChromeClient() {

          // Starting with 5.0, we need to grant permission for the camera when
          // using getUserMedia
          @TargetApi(Build.VERSION_CODES.LOLLIPOP)
          @Override
          public void onPermissionRequest(final PermissionRequest request) {
            request.grant(request.getResources());
          }
        });
        // Allow chrome inspecting for the webview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          WebView.setWebContentsDebuggingEnabled(true);
        }
        // Starting with Android 5.0, this setting was disabled. We need to enable
        // it until we get all sites using SSL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
      }
    });
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public @Nullable Map<String, Integer> getCommandsMap() {
    Map<String, Integer> commandsMap = super.getCommandsMap();

    commandsMap.put("injectBridgeScript", COMMAND_INJECT_BRIDGE_SCRIPT);
    commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);

    return commandsMap;
  }

  @Override
  public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
    super.receiveCommand(root, commandId, args);

    switch (commandId) {
      case COMMAND_INJECT_BRIDGE_SCRIPT:
        injectBridgeScript(root);
        break;
      case COMMAND_SEND_TO_BRIDGE:
        sendToBridge(root, args.getString(0));
        break;
      default:
        //do nothing!!!!
    }
  }

  private void sendToBridge(WebView root, String message) {
    //root.loadUrl("javascript:(function() {\n" + script + ";\n})();");
    String script = "WebViewBridge.onMessage('" + message + "');";
    WebViewBridgeManager.evaluateJavascript(root, script);
  }

  private void injectBridgeScript(WebView root) {
    // this code needs to be executed everytime a url changes.
    WebViewBridgeManager.evaluateJavascript(root, ""
            + "(function() {"
            + "if (window.WebViewBridge) return;"
            + "var customEvent = document.createEvent('Event');"
            + "var WebViewBridge = {"
            + "send: function(message) { WebViewBridgeAndroid.send(message); },"
            + "onMessage: function() {}"
            + "};"
            + "window.WebViewBridge = WebViewBridge;"
            + "customEvent.initEvent('WebViewBridge', true, true);"
            + "document.dispatchEvent(customEvent);"
            + "}());");
  }

  static private void evaluateJavascript(WebView root, String javascript) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
      root.evaluateJavascript(javascript, null);
    } else {
      root.loadUrl("javascript:" + javascript);
    }
  }
}
