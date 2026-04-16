require("dotenv").config();
const express = require("express");
const { MongoClient, ServerApiVersion } = require("mongodb");

const app = express();
app.use(express.json());

const client = new MongoClient(process.env.MONGO_URI, {
  serverApi: {
    version: ServerApiVersion.v1,
    strict: true,
    deprecationErrors: true,
  },
});

let db;

async function conectar() {
  try {
    await client.connect();
    db = client.db("biometrico");
    console.log("Conectado a MongoDB Atlas");
  } catch (error) {
    console.error("Error de conexión:", error);
    process.exit(1);
  }
}

// ── GET / → Ruta raíz
app.get("/", (req, res) => {
  res.json({ status: "ok", mensaje: "Backend Biométrico activo" });
});

// ── POST /entrenamientos → Guardar nuevo entrenamiento
app.post("/entrenamientos", async (req, res) => {
  try {
    const { texto, kilometros, minutos, fecha } = req.body;

    if (!texto || kilometros === undefined || minutos === undefined) {
      return res.status(400).json({ error: "Faltan campos obligatorios" });
    }

    const documento = {
      texto,
      kilometros: parseFloat(kilometros),
      minutos: parseFloat(minutos),
      fecha: fecha || new Date().toISOString(),
      creadoEn: new Date(),
    };

    const resultado = await db
      .collection("entrenamientos")
      .insertOne(documento);

    res.status(201).json({
      mensaje: "Entrenamiento guardado con éxito",
      id: resultado.insertedId,
    });
  } catch (error) {
    res.status(500).json({ error: "Error al guardar: " + error.message });
  }
});

// ── GET /entrenamientos → Obtener historial para gráfica
app.get("/entrenamientos", async (req, res) => {
  try {
    const datos = await db
      .collection("entrenamientos")
      .find({})
      .sort({ creadoEn: -1 })
      .limit(20)
      .toArray();

    res.status(200).json(datos);
  } catch (error) {
    res.status(500).json({ error: "Error al consultar: " + error.message });
  }
});

// ── GET /resumen → Total entrenamientos, km totales, pace promedio
app.get("/resumen", async (req, res) => {
  try {
    const datos = await db
      .collection("entrenamientos")
      .find({})
      .toArray();

    const total = datos.length;
    const kmTotal = datos.reduce((acc, e) => acc + (e.kilometros || 0), 0);
    const minTotal = datos.reduce((acc, e) => acc + (e.minutos || 0), 0);
    const pacePromedio = kmTotal > 0
      ? parseFloat((minTotal / kmTotal).toFixed(1))
      : 0;

    res.status(200).json({
      totalEntrenamientos: total,
      kmTotal: parseFloat(kmTotal.toFixed(1)),
      minTotal: parseFloat(minTotal.toFixed(1)),
      pacePromedio,
    });
  } catch (error) {
    res.status(500).json({ error: "Error al calcular resumen: " + error.message });
  }
});

// ── GET /racha → Días consecutivos de entrenamiento hasta hoy
app.get("/racha", async (req, res) => {
  try {
    const datos = await db
      .collection("entrenamientos")
      .find({})
      .sort({ fecha: -1 })
      .toArray();

    // Extraer fechas únicas en formato YYYY-MM-DD
    const diasUnicos = [
      ...new Set(
        datos.map((e) => {
          const d = new Date(e.fecha || e.creadoEn);
          return d.toISOString().split("T")[0];
        })
      ),
    ].sort((a, b) => b.localeCompare(a)); // más reciente primero

    let racha = 0;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    for (let i = 0; i < diasUnicos.length; i++) {
      const esperado = new Date(hoy);
      esperado.setDate(hoy.getDate() - i);
      const esperadoStr = esperado.toISOString().split("T")[0];

      if (diasUnicos[i] === esperadoStr) {
        racha++;
      } else {
        break;
      }
    }

    res.status(200).json({ racha });
  } catch (error) {
    res.status(500).json({ error: "Error al calcular racha: " + error.message });
  }
});

// ── GET /ping → Verificar que el server vive
app.get("/ping", (req, res) => {
  res.json({ status: "ok", mensaje: "Backend Biométrico activo" });
});

// Iniciar servidor
conectar().then(() => {
  const PORT = process.env.PORT || 3000;
  app.listen(PORT, () => {
    console.log(`Server corriendo en http://localhost:${PORT}`);
  });
});