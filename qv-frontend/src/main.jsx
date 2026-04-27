import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import { createTheme, ThemeProvider, CssBaseline } from '@mui/material'

const darkTheme = createTheme({
    palette: {
        mode: 'dark',
        primary: {
            main: '#90caf9',
        },
        secondary: {
            main: '#f48fb1',
        },
        background: {
            default: '#121212',
            paper: '#1e1e1e',
        },
    },
    shape: {
        borderRadius: 16,
    },
});

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <ThemeProvider theme={darkTheme}>
            <CssBaseline />
            <App />
        </ThemeProvider>
    </React.StrictMode>,
)