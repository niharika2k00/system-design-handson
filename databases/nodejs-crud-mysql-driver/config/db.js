import mysql from "mysql2/promise";

// Method 1: create connection pool
// mysql.createPool({...}).promise();
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  maxIdle: 5, // max idle connections, the default value same as `connectionLimit`
  idleTimeout: 60000, // idle connections timeout, in ms, the default value 60000
  enableKeepAlive: true,
});

// Method 2: create connection
// const connection = mysql.createConnection({
//   host: process.env.DB_HOST,
//   port: process.env.DB_PORT,
//   user: process.env.DB_USER,
//   password: process.env.DB_PASSWORD,
//   database: process.env.DB_NAME,
//   connectTimeout: 60000,
//   connectionLimit: 5,
// });

export default pool;
// export { connection };

// ⚠️ DB connection [TCP] runs immediately on import
