import { Sequelize } from "sequelize";

const sequelize = new Sequelize(process.env.DB_NAME, process.env.DB_USER, process.env.DB_PASSWORD, {
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  dialect: "mysql",
  pool: {
    max: 5,
    min: 0,
    idle: 10000,
  },
  logging: false,
});

export default sequelize;
