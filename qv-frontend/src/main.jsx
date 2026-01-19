import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'

const theme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: '#2481cc',
        },
        secondary: {
            main: '#e64d3d',
        },
        background: {
            default: '#f4f4f5',
            paper: '#ffffff',
        },
    },
    typography: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        h5: { fontWeight: 600 },
        h6: { fontWeight: 500 },
    },
    shape: {
        borderRadius: 12,
    },
});

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <ThemeProvider theme={theme}>
            <CssBaseline /> {/* Сброс CSS браузера */}
            <App />
        </ThemeProvider>
    </React.StrictMode>,
)