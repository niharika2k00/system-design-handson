const products = {
  1: { id: 1, name: "MacBook Pro Laptop", price: 200000, category: "laptops" },
  2: { id: 2, name: "Monitor 32 inch", price: 26000, category: "monitors" },
  3: { id: 3, name: "Trolley", price: 2000, category: "bags" },
};

const db = {
  getProduct: async (id) => {
    console.log(`🗄️  DB hit for product ${id} (slow...)`);
    await new Promise((resolve) => setTimeout(resolve, 1500));
    return products[id] || null;
  },
};

export default db;
