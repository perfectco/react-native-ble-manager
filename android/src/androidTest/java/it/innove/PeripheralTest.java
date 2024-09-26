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

import java.util.Arrays;
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

  public void stubMocks() {
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
    stubMocks();
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
    stubMocks();
    final byte[] data = new byte[MAX_BYTES + 1];
    final InOrder inOrder = inOrder(gatt, characteristic, callback);
    final Peripheral peripheral = new Peripheral(device, reactContext, handler);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    peripheral.write(serviceId, characteristicId, data, MAX_BYTES, 10, callback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

    {
      // verify wrote chunk 1
      inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      inOrder.verify(characteristic).setValue(new byte[MAX_BYTES]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify wrote chunk 2
      inOrder.verify(characteristic).setValue(new byte[1]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify complete
      inOrder.verify(callback).invoke();
    }
  }

  @Test
  public void writeMultipleLarge() {
    stubMocks();
    final byte[] dataA = new byte[MAX_BYTES + 1];
    final byte[] dataB = new byte[MAX_BYTES + 1];
    Arrays.fill(dataB, (byte) 1);
    final Callback callbackA = mock(Callback.class);
    final Callback callbackB = mock(Callback.class);
    final InOrder inOrder = inOrder(gatt, characteristic, callbackA, callbackB);
    final Peripheral peripheral = new Peripheral(device, reactContext, handler);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    // send multiple large messages in quick succession, should enqueue them
    peripheral.write(serviceId, characteristicId, dataA, MAX_BYTES, 10, callbackA, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    peripheral.write(serviceId, characteristicId, dataB, MAX_BYTES, 10, callbackB, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

    // message 1
    {
      // verify wrote chunk 1
      inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      inOrder.verify(characteristic).setValue(new byte[MAX_BYTES]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify wrote chunk 2
      inOrder.verify(characteristic).setValue(new byte[1]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify completed
      inOrder.verify(callbackA).invoke();
    }

    // message 2
    {
      // verify wrote chunk 1
      inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      inOrder.verify(characteristic).setValue(Arrays.copyOfRange(dataB, 0, MAX_BYTES));
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify wrote chunk 2
      inOrder.verify(characteristic).setValue(Arrays.copyOfRange(dataB, MAX_BYTES, MAX_BYTES + 1));
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify completed
      inOrder.verify(callbackB).invoke();
    }
  }

  @Test
  public void doAnythingAfterMultiWrite() {
    stubMocks();
    when(gatt.readRemoteRssi()).thenReturn(true);
    final byte[] data = new byte[MAX_BYTES + 1];
    final Callback writeCallback = mock(Callback.class);
    final Callback rssiCallback = mock(Callback.class);
    final InOrder inOrder = inOrder(gatt, characteristic, writeCallback, rssiCallback);
    final Peripheral peripheral = new Peripheral(device, reactContext, handler);
    peripheral.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

    peripheral.write(serviceId, characteristicId, data, MAX_BYTES, 10, writeCallback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    peripheral.readRSSI(rssiCallback);

    // message 1
    {
      // verify wrote chunk 1
      inOrder.verify(characteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      inOrder.verify(characteristic).setValue(new byte[MAX_BYTES]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify wrote chunk 2
      inOrder.verify(characteristic).setValue(new byte[1]);
      inOrder.verify(gatt).writeCharacteristic(characteristic);

      // ack
      peripheral.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS);

      // verify completed
      inOrder.verify(writeCallback).invoke();
    }

    {
      // verify request RSSI
      inOrder.verify(gatt).readRemoteRssi();

      // read RSSI
      peripheral.onReadRemoteRssi(gatt, -50, BluetoothGatt.GATT_SUCCESS);

      // verify completed
      inOrder.verify(rssiCallback).invoke(null, -50);
    }
  }
}
