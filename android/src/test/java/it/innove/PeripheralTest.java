package it.innove;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.UUID;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
public class PeripheralTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ReactContext reactContext;

  @Mock
  private BluetoothDevice device;

  @Mock
  private BluetoothGatt gatt;

  @Test
  public void basicConstructor() {
    new Peripheral(device, reactContext);
  }

  @Test
  public void write() {
    final UUID serviceId = UUID.randomUUID();
    final UUID characteristicId = UUID.randomUUID();
    final byte[] data = {};
    final Callback callback = mock();
    final Peripheral peripheral = new Peripheral(device, reactContext);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    peripheral.write(serviceId, characteristicId, data, 20, 10, callback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

  }
}
