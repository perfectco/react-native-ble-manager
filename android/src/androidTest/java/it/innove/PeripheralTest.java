package it.innove;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.soloader.SoLoader;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.UUID;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeripheralTest {
  private static final int MAX_BYTES = 20;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ReactContext reactContext;

  @Mock
  private Handler handler;

  @Mock
  private BluetoothDevice device;

  @Mock
  private BluetoothGatt gatt;

  private final UUID serviceId = UUID.randomUUID();

  @Mock
  private BluetoothGattService service;

  private final UUID characteristicId = UUID.randomUUID();

  @Mock
  private BluetoothGattCharacteristic characteristic;

  @Mock
  private Callback callback;

  @BeforeClass
  public static void beforeAll() {
    SoLoader.init(InstrumentationRegistry.getInstrumentation().getTargetContext(), false);
  }

  @Before
  public void beforeEach() {
    when(reactContext.getJSModule(any())).thenAnswer(i -> mock(i.getArgument(0)));
    when(handler.post(any())).thenAnswer(i -> { i.getArgument(0, Runnable.class).run(); return true; });
    when(gatt.getService(serviceId)).thenReturn(service);
    when(gatt.writeCharacteristic(characteristic)).thenReturn(true);
    when(service.getCharacteristic(characteristicId)).thenReturn(characteristic);
    when(characteristic.getWriteType()).thenReturn(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
  }

  @Test
  public void basicConstructor() {
    new Peripheral(device, reactContext);
  }

  @Test
  public void write() {
    final byte[] data = {};
    final InOrder inOrder = inOrder(gatt, characteristic, callback);
    final Peripheral peripheral = new Peripheral(device, reactContext, handler);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    peripheral.write(serviceId, characteristicId, data, MAX_BYTES, 10, callback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

    inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    inOrder.verify(characteristic).setValue(data);
    inOrder.verify(gatt).writeCharacteristic(characteristic);
    inOrder.verify(callback).invoke();
  }

  @Test
  public void writeLarge() {
    final byte[] data = new byte[MAX_BYTES + 1];
    final InOrder inOrder = inOrder(gatt, characteristic, callback);
    final Peripheral peripheral = new Peripheral(device, reactContext, handler);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    peripheral.write(serviceId, characteristicId, data, MAX_BYTES, 10, callback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);
    peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

    inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    inOrder.verify(characteristic).setValue(new byte[MAX_BYTES]);
    inOrder.verify(gatt).writeCharacteristic(characteristic);
    inOrder.verify(characteristic).setValue(new byte[1]);
    inOrder.verify(gatt).writeCharacteristic(characteristic);
    inOrder.verify(callback).invoke();
  }
}
