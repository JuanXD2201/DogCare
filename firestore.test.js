import { test, before, after, beforeEach } from 'node:test';
import assert from 'node:assert';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from '@firebase/rules-unit-testing';

let testEnv;

before(async () => {
  const rules = readFileSync('firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-dogcare-test',
    firestore: {
      rules,
      host: '127.0.0.1',
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test('Public can read products catalog', async () => {
  const unauth = testEnv.unauthenticatedContext();
  const db = unauth.firestore();
  await assertSucceeds(db.collection('products').get());
});

test('Unauthenticated user CANNOT create order', async () => {
  const unauth = testEnv.unauthenticatedContext();
  const db = unauth.firestore();
  await assertFails(
    db.collection('orders').add({
      id: 'ord1',
      userId: 'user123',
      totalCop: 50000,
      paymentStatus: 'PENDING',
      deliveryAddress: 'Calle 100',
    })
  );
});

test('User CANNOT read another user private messages', async () => {
  const user1 = testEnv.authenticatedContext('user1');
  const user2 = testEnv.authenticatedContext('user2');

  // Create conversation between user1 and user3
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('conversations').doc('conv1').set({
      id: 'conv1',
      participantUids: ['user1', 'user3'],
      createdAt: new Date(),
    });
    await adminDb.collection('conversations').doc('conv1').collection('messages').doc('msg1').set({
      id: 'msg1',
      conversationId: 'conv1',
      senderUid: 'user1',
      text: 'Secret message',
      createdAt: new Date(),
    });
  });

  const db2 = user2.firestore();
  await assertFails(
    db2.collection('conversations').doc('conv1').collection('messages').get()
  );
});

test('User CANNOT elevate role to admin on profile creation', async () => {
  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  await assertFails(
    db.collection('users').doc('user1').set({
      userId: 'user1',
      displayName: 'Attacker',
      email: 'attacker@example.com',
      role: 'admin',
    })
  );
});

test('User CAN create profile with role user', async () => {
  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  await assertSucceeds(
    db.collection('users').doc('user1').set({
      userId: 'user1',
      displayName: 'Normal User',
      email: 'user1@example.com',
      role: 'user',
    })
  );
});

test('Stock reduction: succeeds only in PENDING to APPROVED transition; fails with already APPROVED order', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    // Create product p1 with stock 10
    await adminDb.collection('products').doc('p1').set({
      id: 'p1',
      name: 'Dog Food',
      brand: 'BrandX',
      category: 'dry_food',
      priceCop: 50000,
      stock: 10,
      species: 'dog',
      active: true,
    });
    // Create pending order 'ordPending1'
    await adminDb.collection('orders').doc('ordPending1').set({
      id: 'ordPending1',
      userId: 'user1',
      totalCop: 100000,
      paymentStatus: 'PENDING',
      deliveryAddress: 'Calle 100',
      quantities: { p1: 2 },
    });
    // Create already approved order 'ordAlreadyApproved'
    await adminDb.collection('orders').doc('ordAlreadyApproved').set({
      id: 'ordAlreadyApproved',
      userId: 'user1',
      totalCop: 50000,
      paymentStatus: 'APPROVED',
      paymentReference: 'REF-OLD',
      deliveryAddress: 'Calle 100',
      quantities: { p1: 1 },
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // 1. Stock baja solo en la transición PENDING->APPROVED: pasa
  const batch1 = db.batch();
  batch1.update(db.collection('orders').doc('ordPending1'), {
    paymentStatus: 'APPROVED',
    paymentReference: 'REF-NEW-123',
    updatedAt: new Date(),
  });
  batch1.update(db.collection('products').doc('p1'), {
    stock: 8, // 10 - 2 = 8
    lastOrderId: 'ordPending1',
    updatedAt: new Date(),
  });
  await assertSucceeds(batch1.commit());

  // 2. Stock baja con pedido ya aprobado: falla
  const batch2 = db.batch();
  batch2.update(db.collection('products').doc('p1'), {
    stock: 7,
    lastOrderId: 'ordAlreadyApproved',
    updatedAt: new Date(),
  });
  await assertFails(batch2.commit());
});

test('Normal user CANNOT modify totalCop, items or userId on order update', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('orders').doc('ord1').set({
      id: 'ord1',
      userId: 'user1',
      customerName: 'User One',
      deliveryAddress: 'Calle 123',
      items: [{ productId: 'p1', productName: 'Food', unitPriceCop: 50000, quantity: 1, subtotalCop: 50000 }],
      totalCop: 50000,
      paymentStatus: 'PENDING',
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // Updating payment status succeeds with reference
  await assertSucceeds(
    db.collection('orders').doc('ord1').update({
      paymentStatus: 'APPROVED',
      paymentReference: 'REF123',
      updatedAt: new Date(),
    })
  );

  // Changing totalCop fails
  await assertFails(
    db.collection('orders').doc('ord1').update({
      totalCop: 10,
    })
  );

  // Changing userId fails
  await assertFails(
    db.collection('orders').doc('ord1').update({
      userId: 'attacker',
    })
  );
});

test('Payment creation with wrong amount fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('orders').doc('ord10').set({
      id: 'ord10',
      userId: 'user1',
      totalCop: 50000,
      paymentStatus: 'PENDING',
      deliveryAddress: 'Calle 100',
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // Payment with wrong amountCop (10000 instead of 50000)
  const batch = db.batch();
  batch.update(db.collection('orders').doc('ord10'), {
    paymentStatus: 'APPROVED',
    paymentReference: 'REF123',
  });
  batch.set(db.collection('payments').doc('pay1'), {
    id: 'pay1',
    orderId: 'ord10',
    userId: 'user1',
    amountCop: 10000, // Mismatched amount!
    provider: 'PASARELA_SANDBOX',
    status: 'APPROVED',
    paymentReference: 'REF123',
    createdAt: new Date(),
  });
  await assertFails(batch.commit());
});

test('Double state transition on order fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('orders').doc('ordApproved').set({
      id: 'ordApproved',
      userId: 'user1',
      totalCop: 50000,
      paymentStatus: 'APPROVED',
      paymentReference: 'REF123',
      deliveryAddress: 'Calle 100',
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // Transition from APPROVED to DECLINED fails
  await assertFails(
    db.collection('orders').doc('ordApproved').update({
      paymentStatus: 'DECLINED',
      paymentReference: 'REF456',
    })
  );
});

test('APPROVED order transition without paymentReference fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('orders').doc('ordPending').set({
      id: 'ordPending',
      userId: 'user1',
      totalCop: 50000,
      paymentStatus: 'PENDING',
      deliveryAddress: 'Calle 100',
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // APPROVED without paymentReference fails
  await assertFails(
    db.collection('orders').doc('ordPending').update({
      paymentStatus: 'APPROVED',
      paymentReference: '',
    })
  );
});

test('Reading another user private profile fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('users').doc('userPrivate').set({
      userId: 'userPrivate',
      displayName: 'Secret User',
      email: 'secret@example.com',
      role: 'user',
    });
  });

  const user2 = testEnv.authenticatedContext('user2');
  const db = user2.firestore();

  // user2 reading userPrivate fails
  await assertFails(
    db.collection('users').doc('userPrivate').get()
  );
});

test('Message creation with fake senderUid fails', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('conversations').doc('conv10').set({
      id: 'conv10',
      participantUids: ['user1', 'user2'],
    });
  });

  const user1 = testEnv.authenticatedContext('user1');
  const db = user1.firestore();

  // user1 trying to post message with senderUid: 'user2' fails
  await assertFails(
    db.collection('conversations').doc('conv10').collection('messages').doc('msgFake').set({
      id: 'msgFake',
      conversationId: 'conv10',
      senderUid: 'user2', // Fake senderUid!
      text: 'Impersonated message',
      createdAt: new Date(),
    })
  );
});

test('User cart: owner can read and write; foreign cart access fails', async () => {
  const user1 = testEnv.authenticatedContext('user1');
  const user2 = testEnv.authenticatedContext('user2');

  const db1 = user1.firestore();
  const db2 = user2.firestore();

  // Owner user1 can write own cart: pasa
  await assertSucceeds(
    db1.collection('carts').doc('user1').set({
      items: [
        {
          quantity: 2,
          product: { id: 'p1', name: 'Croquetas', priceCop: 50000 }
        }
      ],
      updatedAt: new Date()
    })
  );

  // Owner user1 can read own cart: pasa
  await assertSucceeds(
    db1.collection('carts').doc('user1').get()
  );

  // Foreign user2 cannot read user1 cart: falla
  await assertFails(
    db2.collection('carts').doc('user1').get()
  );

  // Foreign user2 cannot write user1 cart: falla
  await assertFails(
    db2.collection('carts').doc('user1').set({
      items: [],
      updatedAt: new Date()
    })
  );
});


const sellerProduct = (id, sellerUid, extra = {}) => ({
  id,
  sellerUid,
  sellerName: 'Vendedor ' + sellerUid,
  name: 'Alimento ' + id,
  brand: 'Marca',
  category: 'dry_food',
  priceCop: 50000,
  stock: 10,
  species: 'dog',
  active: true,
  ...extra,
});

test('Any signed-in user CAN publish own dog-food product; unauthenticated cannot', async () => {
  const seller = testEnv.authenticatedContext('seller1').firestore();
  await assertSucceeds(seller.collection('products').doc('p1').set(sellerProduct('p1', 'seller1')));

  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection('products').doc('p2').set(sellerProduct('p2', 'seller1')));
});

test('User CANNOT publish a product in the name of another seller', async () => {
  const user = testEnv.authenticatedContext('user1').firestore();
  await assertFails(user.collection('products').doc('p1').set(sellerProduct('p1', 'someoneElse')));
});

test('Product with species other than dog, invalid category, or negative price fails', async () => {
  const seller = testEnv.authenticatedContext('seller1').firestore();
  await assertFails(seller.collection('products').doc('pCat').set(sellerProduct('pCat', 'seller1', { species: 'cat' })));
  await assertFails(seller.collection('products').doc('pToy').set(sellerProduct('pToy', 'seller1', { category: 'toys' })));
  await assertFails(seller.collection('products').doc('pNeg').set(sellerProduct('pNeg', 'seller1', { priceCop: -5 })));
});

test('Only the seller can edit or delete own product; other users and sellerUid changes fail', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('products').doc('p1').set(sellerProduct('p1', 'seller1'));
  });

  const seller = testEnv.authenticatedContext('seller1').firestore();
  const other = testEnv.authenticatedContext('other1').firestore();

  await assertFails(other.collection('products').doc('p1').update({ priceCop: 1 }));
  await assertFails(other.collection('products').doc('p1').delete());

  // El vendedor edita su producto pero no puede cederlo a otro UID
  await assertSucceeds(seller.collection('products').doc('p1').update({ priceCop: 60000 }));
  await assertFails(seller.collection('products').doc('p1').update({ sellerUid: 'other1' }));

  await assertSucceeds(seller.collection('products').doc('p1').delete());
});

test('Legacy product without sellerUid cannot be edited or deleted from the app', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('products').doc('legacy').set({
      id: 'legacy', name: 'Viejo', brand: 'X', category: 'dry_food', priceCop: 1, stock: 1, active: true,
    });
  });
  const user = testEnv.authenticatedContext('user1').firestore();
  await assertFails(user.collection('products').doc('legacy').update({ priceCop: 2 }));
  await assertFails(user.collection('products').doc('legacy').delete());
});

test('Categories: public can read; nobody can write from the client, not even admin', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('users').doc('admin1').set({
      userId: 'admin1', displayName: 'Admin', email: 'admin@example.com', role: 'admin',
    });
    await context.firestore().collection('categories').doc('dry_food').set({ id: 'dry_food', name: 'Seco', order: 1 });
  });

  await assertSucceeds(testEnv.unauthenticatedContext().firestore().collection('categories').doc('dry_food').get());
  const newCat = { id: 'wet_food', name: 'Húmedo', order: 2 };
  await assertFails(testEnv.authenticatedContext('user1').firestore().collection('categories').doc('wet_food').set(newCat));
  await assertFails(testEnv.authenticatedContext('admin1').firestore().collection('categories').doc('wet_food').set(newCat));
});

test('Blocking: only the owner can read or write own blockedUsers', async () => {
  const user1 = testEnv.authenticatedContext('user1').firestore();
  const user2 = testEnv.authenticatedContext('user2').firestore();

  await assertSucceeds(
    user1.collection('users').doc('user1').collection('blockedUsers').doc('user2').set({
      blockedUid: 'user2', createdAt: new Date(),
    })
  );
  await assertSucceeds(user1.collection('users').doc('user1').collection('blockedUsers').get());

  // Otro usuario no puede leer la lista ni bloquear en nombre de user1
  await assertFails(user2.collection('users').doc('user1').collection('blockedUsers').get());
  await assertFails(
    user2.collection('users').doc('user1').collection('blockedUsers').doc('user3').set({
      blockedUid: 'user3', createdAt: new Date(),
    })
  );
  // No puede bloquearse a sí mismo
  await assertFails(
    user1.collection('users').doc('user1').collection('blockedUsers').doc('user1').set({
      blockedUid: 'user1', createdAt: new Date(),
    })
  );
});

test('A blocked user CANNOT send messages to the blocker; non-blocked can', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection('conversations').doc('c12').set({ id: 'c12', participantUids: ['user1', 'user2'] });
    await db.collection('conversations').doc('c13').set({ id: 'c13', participantUids: ['user1', 'user3'] });
    await db.collection('users').doc('user1').collection('blockedUsers').doc('user2').set({
      blockedUid: 'user2', createdAt: new Date(),
    });
  });

  const msg = (id, convId, sender) => ({
    id, conversationId: convId, senderUid: sender, text: 'hola', createdAt: new Date(),
  });

  const user2 = testEnv.authenticatedContext('user2').firestore();
  await assertFails(user2.collection('conversations').doc('c12').collection('messages').doc('m1').set(msg('m1', 'c12', 'user2')));

  const user3 = testEnv.authenticatedContext('user3').firestore();
  await assertSucceeds(user3.collection('conversations').doc('c13').collection('messages').doc('m2').set(msg('m2', 'c13', 'user3')));
});

test('A blocked user CANNOT start a new conversation with the blocker', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('users').doc('user1').collection('blockedUsers').doc('user2').set({
      blockedUid: 'user2', createdAt: new Date(),
    });
  });
  const user2 = testEnv.authenticatedContext('user2').firestore();
  await assertFails(
    user2.collection('conversations').doc('cNew').set({ id: 'cNew', participantUids: ['user2', 'user1'] })
  );
});

test('Reports: reporter can create with own uid; spoofed reporter fails; non-admin cannot read', async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection('users').doc('admin1').set({
      userId: 'admin1', displayName: 'Admin', email: 'admin@example.com', role: 'admin',
    });
  });

  const report = (id, reporter) => ({
    id, reporterUid: reporter, reportedUid: 'user2', conversationId: 'c12', reason: 'Spam', createdAt: new Date(),
  });

  const user1 = testEnv.authenticatedContext('user1').firestore();
  await assertSucceeds(user1.collection('reports').doc('r1').set(report('r1', 'user1')));
  await assertFails(user1.collection('reports').doc('r2').set(report('r2', 'userX'))); // reporterUid falso
  await assertFails(user1.collection('reports').doc('r1').get()); // no admin
  await assertFails(
    user1.collection('reports').doc('r3').set({ ...report('r3', 'user1'), reason: 'x'.repeat(501) })
  );

  const admin = testEnv.authenticatedContext('admin1').firestore();
  await assertSucceeds(admin.collection('reports').doc('r1').get());
});
